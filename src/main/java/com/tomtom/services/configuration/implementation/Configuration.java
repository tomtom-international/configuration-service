/*
 * Copyright (C) 2016. TomTom International BV. All rights reserved.
 */

package com.tomtom.services.configuration.implementation;

import com.fasterxml.jackson.core.JsonParser.Feature;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.dataformat.xml.XmlMapper;
import com.google.common.base.Joiner;
import com.google.common.base.Splitter;
import com.tomtom.services.configuration.ConfigurationServiceProperties;
import com.tomtom.services.configuration.domain.Node;
import com.tomtom.services.configuration.dto.NodeDTO;
import com.tomtom.services.configuration.dto.SearchResultDTO;
import com.tomtom.services.configuration.dto.SearchResultsDTO;
import com.tomtom.speedtools.apivalidation.exceptions.ApiException;
import com.tomtom.speedtools.apivalidation.exceptions.ApiParameterSyntaxException;
import com.tomtom.speedtools.objects.Immutables;
import com.tomtom.speedtools.objects.Tuple;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import javax.inject.Inject;
import javax.ws.rs.core.Response.Status;
import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Set;
import java.util.regex.Pattern;

import static com.tomtom.services.configuration.TreeResource.*;
import static com.tomtom.speedtools.objects.Objects.notNullOr;

/**
 * This class implements the search tree, which consists of nodes and leafs. Every node can have
 * 0 or more children nodes and 0 or 1 leaf node. There is a single root node.
 *
 * Nodes have a name, an optional list of child nodes and an optional leaf with parameters.
 * Node names are unique within children nodes and cannot be empty, except for the root node name
 * which is empty.
 *
 * Nodes cannot be renamed, although you can replace nodes with other nodes (with another name).
 */
@SuppressWarnings("EqualsWhichDoesntCheckParameterClass")
public class Configuration {
    private static final Logger LOG = LoggerFactory.getLogger(Configuration.class);

    private final boolean initialConfigurationOK;

    /**
     * The root node of the tree.
     */
    @Nonnull
    private final Node root;

    /**
     * The URL to read the configuration tree from.
     */
    @Nonnull
    private final ConfigurationServiceProperties configurationServiceProperties;

    @Inject
    public Configuration(@Nonnull final ConfigurationServiceProperties configurationServiceProperties)
            throws IncorrectConfigurationException {

        // Call the helper constructor and read the configuration as one large string.
        this(configurationServiceProperties,
                readConfiguration(configurationServiceProperties.getStartupConfigurationURI()));
    }

    /**
     * Package private. Constructor used for testing the class. Allows you to inject a string configuration directly.
     * This constructor is also used by the "real" external constructor to pass the configuration file contents.
     *
     * @param configurationServiceProperties Configuration.
     * @param overrideStartupConfiguration   String configuration which overrides the configuration URL.
     *                                       Note that the regular constructor uses this as well.
     */
    Configuration(
            @Nonnull final ConfigurationServiceProperties configurationServiceProperties,
            @Nullable final String overrideStartupConfiguration)
            throws IncorrectConfigurationException {

        // Create an empty root.
        NodeDTO realRoot = new NodeDTO(null, null, null, null, null, null);
        boolean realInitialConfigurationOK = false;
        this.configurationServiceProperties = configurationServiceProperties;

        // If the configuration is specified as a parameter (in tests), use that one.
        if (overrideStartupConfiguration != null) {
            try {

                // Read the tree and validate.
                final NodeDTO root = getRootNodeAndValidateTree(
                        configurationServiceProperties.getStartupConfigurationURI(),
                        overrideStartupConfiguration);
                LOG.info("Tree: Startup configuration read OK, startupConfiguration={}", root);

                // Use the root just read as the real root.
                realRoot = root;
                realInitialConfigurationOK = true;
            } catch (final ApiException | IncorrectConfigurationException e) {
                LOG.error("Tree: Startup configuration cannot be read: {}", e.getMessage());
                throw new IncorrectConfigurationException(e.getMessage());
            }
        }

        // Convert the DTO tree to a domain tree.
        this.root = new Node(realRoot, null);
        this.initialConfigurationOK = realInitialConfigurationOK;
    }

    /**
     * Return if startup configuration was correctly read or not. This may be used at startup time to prevent
     * the service from booting up.
     *
     * @return True if the config is OK.
     */
    public boolean isStartupConfigurationOK() {
        return initialConfigurationOK;
    }

    /**
     * Return the URI of the start-up configuration.
     *
     * @return URI of start-up configuration.
     */
    @Nonnull
    public String getStartupConfigurationURI() {
        return configurationServiceProperties.getStartupConfigurationURI();
    }

    /**
     * Get the root node.
     *
     * @return Root node. Has an empty name.
     */
    @Nonnull
    public Node getRoot() {
        return root;
    }

    /**
     * Find the deepest node which matches the provide search path and which has a leaf with parameters
     * attached to it.
     *
     * @param levels      Order of level names.
     * @param searchPaths Search paths, separated by path separator '/'. Multiple queries (search paths) separated by ','.
     * @return Empty list if no matching node was found. Otherwise a list of tuples with the parameters of the deepest node found
     * and the full path to the matching node.
     */
    @Nonnull
    SearchResultsDTO findBestMatchingNodes(
            @Nonnull final String levels,
            @Nonnull final String searchPaths) {

        // Index mapper from input order of node levels to root configuration order.
        final List<Integer> queryToTreeMapper = new ArrayList<>();

        // Re-order the search criteria to match the order of the configuration tree.
        final Iterable<String> queryLevels = Splitter.on(SEPARATOR_PATH).trimResults().split(levels);
        if (root.getLevels() != null) {

            // Look up every level listed in the query. Their names must exist.
            for (final String queryLevel : queryLevels) {
                boolean found = false;
                int index = 0;
                for (final String level : root.getLevels()) {
                    if (queryLevel.equalsIgnoreCase(level)) {
                        queryToTreeMapper.add(index);
                        found = true;
                        break;
                    }
                    ++index;
                }

                // Check if an unknown name was used in the query.
                if (!found) {
                    throw new ApiParameterSyntaxException(QUERY_PARAM_LEVELS, queryLevel, "Name must be one of: " + Joiner.on(", ").join(root.getLevels()));
                }
            }
        }

        // Separate the individual queries from the input (one input string may contains multiple search queries).
        final Iterable<String> querySearches = Splitter.on(SEPARATOR_QUERY).trimResults().split(searchPaths);

        // Reshuffle the search terms as specified in the query to what we expect in the configuration tree.
        final List<String> searches = new ArrayList<>();

        // Reshuffle all queries.
        for (final String querySearch : querySearches) {

            // Make sure the query does not contains ',', just '/'.
            if (querySearch.contains(",")) {
                throw new ApiParameterSyntaxException(QUERY_PARAM_SEARCH, querySearch, "Should not contain ',' (term separator is " + SEPARATOR_PATH + ')');
            }
            // Get original terms and reshuffle into newTerms.
            final Iterable<String> queryTerms = Splitter.on(SEPARATOR_PATH).trimResults().split(querySearch);
            LOG.debug("findBestMatchingNodes: querySearch={}, queryTerms={}", querySearch, queryTerms.toString());

            // Init tree terms.
            final String[] treeTerms = new String[queryToTreeMapper.size()];
            for (int i = 0; i < treeTerms.length; ++i) {
                treeTerms[i] = "";
            }

            // Copy correct values in tree terms.
            final Iterator<String> iterator = queryTerms.iterator();
            int index = 0;
            while (iterator.hasNext() && (index < treeTerms.length)) {
                treeTerms[queryToTreeMapper.get(index)] = iterator.next();
                ++index;
            }
            if (iterator.hasNext()) {
                throw new ApiParameterSyntaxException(QUERY_PARAM_SEARCH, querySearch, "Must be no more than " + queryToTreeMapper.size() + " levels deep");
            }

            // Add reshuffled joined terms.
            searches.add(Joiner.on(SEPARATOR_PATH).join(treeTerms));
        }

        // Result list.
        final List<SearchResultDTO> results = new ArrayList<>();

        /**
         * Split the comma-separated queries, which is something 'path1/path2,path2/path3' into
         * separate queries, like 'path1/path2' and 'path3/path4'.
         */
        for (final String search : searches) {
            LOG.debug("findBestMatchingNodes: search={}", search);

            /*
             * Search tree for parameters. Start with assuming the search fails and the result is
             * the ultimate fallback: the parameters of the root node.
             */
            Node nodeOfParameters = root;       // This points at the node the parameters were taken from.
            Node nodeToCheck = root;            // This points at the node to we need to dive into.

            /**
             * Split the slash-separate search terms, like 'path1/path2' into separate terms, like
             * 'path1' and 'path2'.
             */
            for (final String searchTerm : Splitter.on(SEPARATOR_PATH).trimResults().split(search)) {
                LOG.debug("findBestMatchingNodes: searchTerm={}", searchTerm);
                boolean found = false;          // This indicates whether we found a match or not.

                /**
                 * Check all children nodes of this node (if they exist).
                 */
                final Collection<Node> children = nodeToCheck.getNodes();
                if (children != null) {

                    /**
                     * First check all 'exact' (non-regex) matches. If the string match is exact,
                     * regular expression matches will not be checked.
                     */
                    final List<Node> nonExactMatches = new ArrayList<>();
                    for (final Node child : children) {

                        // The name of children is a regex.
                        final String name = child.getName();
                        assert name != null;

                        // Check if the term matches the node name literally.
                        //noinspection ConstantConditions
                        if (searchTerm.matches(createCaseInsensitivePattern(Pattern.quote(name)))) {
                            found = true;

                            /**
                             * Remember the parameters of this child node, as it is more specific than the
                             * one kept until now.
                             */
                            if (child.getParameters() != null) {
                                nodeOfParameters = child;
                            }

                            // Start next search in this subtree.
                            nodeToCheck = child;
                            break;
                        } else {

                            // Keep this node for second round, checking regex matches.
                            nonExactMatches.add(child);
                        }
                    }

                    // Second round: only if no exact match was found, check regular expressions.
                    if (!found) {
                        for (final Node child : nonExactMatches) {

                            // The name of children is a regex.
                            final String name = child.getName();
                            assert name != null;

                            //noinspection ConstantConditions
                            if (searchTerm.matches(createCaseInsensitivePattern(name))) {
                                found = true;

                                /**
                                 * Remember the parameters of this child node, as it is more specific than the
                                 * one kept until now.
                                 */
                                if (child.getParameters() != null) {
                                    nodeOfParameters = child;
                                }

                                // Start next search in this subtree.
                                nodeToCheck = child;
                                break;
                            }
                        }
                    }
                }

                // Stop searching for deeper path terms if we couldn't find a match for this term.
                if (!found) {
                    break;
                }
            }

            final SearchResultDTO searchResult;
            //noinspection ObjectEquality
            if (nodeOfParameters == root) {

                if (root.getParameters() == null) {

                    /**
                     * If no parameters were found, anywhere, then return an empty list. This indicates at least one
                     * of the queries was not successful. The other queries will not even be executed.
                     */
                    return new SearchResultsDTO(Immutables.emptyList());
                } else {

                    // Return the non-null root parameters as a fallback if no matches were found.
                    searchResult = new SearchResultDTO(root);
                }
            } else {

                // Return the non-null parameters of the found node.
                searchResult = new SearchResultDTO(nodeOfParameters);
            }

            /**
             * Set the 'matched' of the node from which the parameters were gotten.
             */
            final String matched = getPathOfNode(root, nodeOfParameters, "").getValue1();
            searchResult.setMatched(matched.isEmpty() ? "/" : matched);
            results.add(searchResult);
        }
        final SearchResultsDTO searchResults = new SearchResultsDTO(results);
        return searchResults;
    }

    /**
     * Given a full node path, return the node and its parent node, or null.
     *
     * Important: If the root node is found, the parent node is ALSO the root node. This is primarily because
     * you cannot return a null value in a tuple.
     *
     * @param fullNodePath Full path to a node, separated by separators.
     * @return Null if not found. Otherwise a tuple with the node found (value 1) and its parent node (value 2).
     */
    @Nullable
    Node findNode(@Nonnull final String fullNodePath) {

        // Trim path.
        final String trimmedFullNodePath = fullNodePath.trim();

        // Return root node if path is empty.
        if (trimmedFullNodePath.isEmpty()) {

            // Important: root has no parent, but you cannot return null as a parent either, so return root as well.
            return root;
        }

        // Search tree for right node.
        Node node = root;
        for (final String sub : Splitter.on(SEPARATOR_PATH).trimResults().split(trimmedFullNodePath)) {
            boolean found = false;
            final Collection<Node> children = node.getNodes();
            if (children != null) {
                for (final Node child : children) {
                    final String name = notNullOr(child.getName(), "");
                    if (name.equals(sub)) {
                        found = true;
                        node = child;
                        break;
                    }
                }
            }

            if (!found) {
                return null;
            }
        }
        return node;
    }

    /**
     * Get the full path name of a node. Note that the node object itself will be searched for, so the method
     * will use an object equality test to find a specific node, not an equals() test.
     *
     * @param tree       Tree to search the node in.
     * @param node       Node to search for.
     * @param pathPrefix Path to be used as prefix (without trailing '/').
     *                   This is supplied to be able to make the method recursively callable.
     * @return A tuple with as value 1 the path name of the node, within the specified tree (with the specified path
     * prefix, and as value 2 a boolean which indicates whether the node was found or not. If not, the returned path
     * equals the path prefix.
     */
    @Nonnull
    private static Tuple<String, Boolean> getPathOfNode(
            @Nonnull final Node tree,
            @Nullable final Node node,
            @Nonnull final String pathPrefix) {
        if (tree.getNodes() != null) {
            for (final Node child : tree.getNodes()) {
                final String name = notNullOr(child.getName(), "");
                //noinspection ObjectEquality
                if (child == node) {
                    return new Tuple<>(pathPrefix + SEPARATOR_PATH + name, true);
                } else {
                    final Tuple<String, Boolean> found = getPathOfNode(child, node, pathPrefix + SEPARATOR_PATH + name);
                    if (found.getValue2()) {
                        return found;
                    }
                }
            }
        }
        return new Tuple<>(pathPrefix, false);
    }

    /**
     * Read a configuration from a URI, which may be prefixed http:, https:, file: or classpath:.
     * The configuration is returned as a single concatenated string.
     *
     * @param uri URI to read from.
     * @return Concatenated input lines, or null if reading the configuration failed.
     */
    @Nonnull
    private static String readConfiguration(@Nonnull final String uri) throws IncorrectConfigurationException {
        InputStreamReader inputStreamReader = null;
        try {
            if (uri.startsWith("http:") || uri.startsWith("https:")) {
                final URL httpURL = new URL(uri);
                final HttpURLConnection connection = (HttpURLConnection) httpURL.openConnection();
                connection.setRequestMethod("GET");

                final int responseCode = connection.getResponseCode();
                if (responseCode != Status.OK.getStatusCode()) {
                    throw new IncorrectConfigurationException("Could not read startup configuration, uri=" + uri + ", responseCode={}" + responseCode);
                }
                //noinspection IOResourceOpenedButNotSafelyClosed
                inputStreamReader = new InputStreamReader(connection.getInputStream());

            } else if (uri.startsWith("file:")) {
                final String filename = uri.replaceFirst("file::?", "");
                LOG.debug("readConfiguration: read file={}", filename);
                //noinspection IOResourceOpenedButNotSafelyClosed
                inputStreamReader = new FileReader(filename);

            } else if (uri.startsWith("classpath:")) {
                final String filename = uri.replaceFirst("classpath::?", "");
                LOG.debug("readConfiguration: read from classpath={}", filename);
                final InputStream resourceAsStream = Configuration.class.getClassLoader().getResourceAsStream(filename);
                if (resourceAsStream == null) {
                    throw new IncorrectConfigurationException("File not found on classpath: uri=" + uri);
                }
                //noinspection IOResourceOpenedButNotSafelyClosed
                inputStreamReader = new InputStreamReader(resourceAsStream);

            } else {
                throw new IncorrectConfigurationException("Unknown protocol, must specify 'http:', 'https:', 'file:', or 'classpath:'.");
            }
        } catch (final IOException e) {
            LOG.warn("readConfiguration: {}, message={}", uri, e.getMessage());
            try {
                assert inputStreamReader != null;
                //noinspection ConstantConditions
                inputStreamReader.close();
            } catch (final IOException ignored) {
                // Ignored.
            }
            throw new IncorrectConfigurationException("Cannot read configuration, url=" + uri + ", exception=" + e.getMessage());
        }

        // Read input stream.
        final StringBuilder sb = new StringBuilder();
        try {
            //noinspection NestedTryStatement
            try (BufferedReader buffer = new BufferedReader(inputStreamReader)) {
                String inputLine;
                while ((inputLine = buffer.readLine()) != null) {
                    LOG.trace("readConfiguration: {}", inputLine);
                    sb.append(inputLine);
                    sb.append('\n');
                }
            }
        } catch (final IOException e) {
            throw new IncorrectConfigurationException("readConfiguration: Could not parse configuration, uri={}" + uri +
                    ", response=" + sb + ", message=" + e.getMessage());
        }
        return sb.toString();
    }

    @Nonnull
    private static NodeDTO getRootNodeAndValidateTree(
            @Nonnull final String include,
            @Nonnull final String content)
            throws IncorrectConfigurationException {

        // Read the tree from the configuration.
        final NodeDTO root = getChildNodeFromConfiguration(include, content, new ArrayList<>());

        // Check if the name of the root is null; all child names have been checked by now (when tree was read in).
        if (root.getName() != null) {
            throw new IncorrectConfigurationException("Configuration is not OK! Top-level root node must be nameless.");
        }

        // Check 'levels' specification.
        if (root.getLevels() == null) {

            // The 'levels' element can only be empty if the configuration is empty.
            if (root.getNodes() != null) {
                throw new IncorrectConfigurationException("No 'levels' found: unique names must be specified for all node levels.");
            }
        } else {

            // Check correctness of level names.
            final Set<String> levels = new HashSet<>();
            for (final String level : root.getLevels()) {

                // Level name must be non-empty.
                if (level.isEmpty()) {
                    throw new IncorrectConfigurationException("Level name cannot be empty.");
                }

                // Level name cannot contain certain characters, like [,;/].
                if (!isValidNodeName(level)) {
                    throw new IncorrectConfigurationException("Level name cannot contain '" + SEPARATOR_WRONG +
                            "', '" + SEPARATOR_PATH + "' or '" + SEPARATOR_QUERY + "' and cannot be named '" +
                            QUERY_PARAM_LEVELS + "' or '" + QUERY_PARAM_SEARCH + "'.");
                }

                // Level names must be unique.
                if (levels.contains(level.toLowerCase())) {
                    throw new IncorrectConfigurationException("Level name '" + level + "' was specified more than once.");
                }
                levels.add(level.toLowerCase());
            }

            // If the levels are specified there must be at least as many order names as there are node levels.
            final int deepestLevel = deepestNodeLevel(root.getNodes(), 1);
            if (root.getLevels().size() < deepestLevel) {
                throw new IncorrectConfigurationException("Incorrect number of 'levels' specified, expecting at least " + deepestLevel + " levels");
            }
        }

        // Validate the root (and all of its children).
        root.validate();
        return root;
    }

    private static boolean isValidNodeName(@Nonnull final String nodeName) {
        return ((nodeName.indexOf(SEPARATOR_WRONG) + nodeName.indexOf(SEPARATOR_PATH) + nodeName.indexOf(SEPARATOR_QUERY)) == -3) &&
                !nodeName.equalsIgnoreCase(QUERY_PARAM_LEVELS) && !nodeName.equalsIgnoreCase(QUERY_PARAM_SEARCH);
    }

    @Nonnull
    private static NodeDTO getChildNodeFromConfiguration(
            @Nonnull final String include,
            @Nonnull final String content,
            @Nonnull final List<String> included)
            throws IncorrectConfigurationException {

        // Read tree as JSON or XML.
        NodeDTO tree;
        try {

            // Try to read as JSON first.
            final ObjectMapper mapper = new ObjectMapper();
            mapper.configure(Feature.ALLOW_COMMENTS, true);
            tree = mapper.readValue(content, NodeDTO.class);
        } catch (final IOException e1) {

            try {

                // If JSON fails, try XML instead.
                final XmlMapper mapper = new XmlMapper();
                tree = mapper.readValue(content, NodeDTO.class);
            } catch (final IOException e2) {

                // Not valid JSON or XML.
                final String msg;
                final String jsonError = e1.getMessage();
                final String xmlError = e2.getMessage();
                if (jsonError.startsWith("Unexpected character ('<'")) {
                    msg = "XML ERROR: " + xmlError;
                } else {
                    msg = "JSON ERROR: " + jsonError;
                }
                throw new IncorrectConfigurationException("Configuration is NOT OK! Should be valid JSON or XML\n" + msg);
            }
        }

        // Inline all includes recursively.
        expandAllIncludes(tree, included);

        // Check if node names do not conflict.
        if (!checkNodeNamesRoot(tree)) {
            throw new IncorrectConfigurationException("Configuration is not OK! " +
                    "Nodes names are incorrectly formatted, not unique or contain incorrect key/value pairs.");
        }

        // Validate the (sub)tree.
        tree.validate();

        LOG.debug("getRootNodeFromConfiguration: Configuration for node '{}' is OK", include);
        return tree;
    }

    /**
     * Return the deepest node level.
     *
     * @param nodes Current level of nodes.
     * @param level Current index of level.
     * @return Deepest level found.
     */
    private static int deepestNodeLevel(@Nullable final Collection<NodeDTO> nodes, final int level) {
        if (nodes == null) {
            // This level wasn't a real level.
            return level - 1;
        }

        // This level is a real level.
        int deepest = level;
        for (final NodeDTO node : nodes) {
            final int newLevel = deepestNodeLevel(node.getNodes(), level + 1);
            deepest = Math.max(deepest, newLevel);
        }
        return deepest;
    }

    /**
     * Give a series of nodes at the same level, check if the names are OK.
     * Check the children as well
     *
     * @param root Root node.
     * @return True if all OK, false otherwise.
     */
    private static boolean checkNodeNamesRoot(@Nonnull final NodeDTO root) {
        return checkNodeNamesChildren(root.getNodes());
    }

    /**
     * Give a series of nodes at the same level, check if the names are OK.
     * Check the children as well
     *
     * @param children Set of nodes.
     * @return True if all OK, false otherwise.
     */
    private static boolean checkNodeNamesChildren(@Nullable final List<NodeDTO> children) {
        if (children == null) {
            return true;
        }
        boolean ok = true;
        final Set<String> names = new HashSet<>();      // Node names (per level).
        for (final NodeDTO child : children) {
            final String name = child.getName();
            if (name == null) {
                ok = false;
                LOG.error("checkNodeNamesChildren: name cannot be null");
            } else if (name.isEmpty()) {
                ok = false;
                LOG.error("checkNodeNamesChildren: name cannot be empty");
            } else if (!isValidNodeName(name)) {
                ok = false;
                LOG.error("checkNodeNamesChildren: incorrect name or format of node");
            } else if (names.contains(name)) {
                ok = false;
                LOG.error("checkNodeNamesChildren: name must be unique, name={}", name);
            } else {
                names.add(name);
                ok = ok && checkNodeNamesChildren(child.getNodes());
            }
        }
        return ok;
    }

    /**
     * Expand all the included subtrees in a node.
     *
     * @param tree     Node to expand.
     * @param included Memory of which include files were processed.
     * @throws IncorrectConfigurationException If include recursion was detected.
     */
    private static void expandAllIncludes(
            @Nonnull final NodeDTO tree,
            @Nonnull final List<String> included) throws IncorrectConfigurationException {
        final String include = tree.getInclude();
        if (include != null) {
            LOG.info("expandAllIncludes: Include specified, include={}", include);

            // Check endless recursion.
            if (included.contains(include)) {
                throw new IncorrectConfigurationException("Endless recursion detected at include=" + include);
            }

            // Push name to stack.
            included.add(0, include);

            // Read JSON content from include.
            final String content = readConfiguration(include);

            // Parse nodes from content.
            final NodeDTO expandedChild = getChildNodeFromConfiguration(include, content, included);

            // Remove include, replace with read node.
            tree.setInclude(null);
            tree.setName(expandedChild.getName());
            tree.setNodes(expandedChild.getNodes());
            tree.setParameters(expandedChild.getParameters());
            tree.setModified(expandedChild.getModified());

            // Expand all includes in children.
            expandAllIncludes(tree, included);

            // Pop name from stack.
            final String removed = included.remove(0);
            assert removed.equals(include);
        } else {

            // Include not specified. Process children.
            final List<NodeDTO> children = tree.getNodes();
            if (children != null) {
                for (final NodeDTO child : children) {
                    expandAllIncludes(child, included);
                }
            }
        }
    }

    /**
     * Create a regex pattern which matches strings case-insensitive.
     *
     * @param pattern Pattern to make case-insensitive.
     * @return Case-insensitive pattern.
     */
    @Nonnull
    private static String createCaseInsensitivePattern(@Nonnull final String pattern) {
        return "(?i:" + pattern + ')';
    }
}
