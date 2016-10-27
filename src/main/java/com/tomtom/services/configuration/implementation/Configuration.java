/*
 * Copyright (C) 2016. TomTom International BV. All rights reserved.
 */

package com.tomtom.services.configuration.implementation;

import com.fasterxml.jackson.core.JsonParser.Feature;
import com.fasterxml.jackson.databind.JavaType;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.type.TypeFactory;
import com.fasterxml.jackson.dataformat.xml.XmlMapper;
import com.google.common.base.Splitter;
import com.tomtom.services.configuration.ConfigurationServiceProperties;
import com.tomtom.services.configuration.domain.Node;
import com.tomtom.services.configuration.dto.NodeDTO;
import com.tomtom.services.configuration.dto.ParameterDTO;
import com.tomtom.services.configuration.dto.ParameterListDTO;
import com.tomtom.services.configuration.dto.SearchResultDTO;
import com.tomtom.services.configuration.dto.SearchResultsDTO;
import com.tomtom.services.configuration.dto.SupportsInclude;
import com.tomtom.speedtools.apivalidation.exceptions.ApiException;
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
import java.util.Arrays;
import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.regex.Pattern;

import static com.google.common.base.Strings.nullToEmpty;
import static com.tomtom.services.configuration.TreeResource.*;
import static com.tomtom.speedtools.objects.Objects.notNullOr;

/**
 * This class implements the search tree, which consists of nodes and leafs. Every node can have
 * 0 or more children nodes and 0 or 1 leaf node. There is a single root node.
 * <p>
 * Nodes have a match string, an optional list of child nodes and an optional leaf with parameters.
 * Node match strings are unique within children nodes and cannot be empty, except for the root node
 * which is absent.
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
        NodeDTO realRoot = new NodeDTO(null, null, null, null, null, null, null);
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
     * @return Root node. Has an empty match strings.
     */
    @Nonnull
    public Node getRoot() {
        return root;
    }

    /**
     * Find the deepest node which matches the provide search path and which has a leaf with parameters
     * attached to it.
     *
     * @param levelSearchTermsList A list of queries, which consists of a map: (level-name: search-term).
     * @return Empty list if no matching node was found. Otherwise a list of tuples with the parameters of the deepest node found
     * and the full path to the matching node.
     */
    @Nonnull
    SearchResultsDTO matchNode(@Nonnull final List<Map<String, String>> levelSearchTermsList) {

        // Result list.
        final List<SearchResultDTO> results = new ArrayList<>();

        // Process all search queries.
        for (final Map<String, String> levelSearchTerms : levelSearchTermsList) {
            LOG.debug("matchNode: search #{}, levelSearchTerms={}", results.size() + 1, levelSearchTerms);

            /*
             * Search tree for parameters. Start with assuming the search fails and the result is
             * the ultimate fallback: the parameters of the root node.
             */
            Node nodeOfParameters = root;       // This points at the node the parameters were taken from.
            Node nodeToCheck = root;            // This points at the node to we need to dive into.
            if (root.getLevels() != null) {     // Only execute search if levels actually exist.

                for (final String levelName : root.getLevels()) {
                    boolean found = false;          // This indicates whether we found a match or not.

                    // Find the corresponding search term in the query.
                    final String searchTerm = nullToEmpty(levelSearchTerms.get(levelName));
                    LOG.debug("matchNode:   {}={}", levelName, searchTerm);

                    /**
                     * Check all children nodes of this node (if they exist).
                     */
                    final Collection<Node> children = nodeToCheck.getNodes();
                    if (children != null) {

                        /**
                         * First check all 'exact' literal (non-regex) matches. If the string match is exact,
                         * regular expression matches will not be checked. This is to make sure that if
                         * a ".*" node is specified "left of" other nodes, it does not overrule literal
                         * matches.
                         */
                        final List<Node> nonExactMatches = new ArrayList<>();
                        for (final Node child : children) {

                            // The name of children is a regex.
                            final String name = child.getMatch();
                            assert name != null;

                            // Check if the term matches the node name literally.
                            //noinspection ConstantConditions
                            if (searchTerm.matches(createCaseInsensitivePattern(Pattern.quote(name)))) {
                                LOG.debug("matchNode:     FOUND, literal match, {}={}", levelName, name);
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
                                final String match = child.getMatch();
                                assert match != null;

                                //noinspection ConstantConditions
                                if (searchTerm.matches(createCaseInsensitivePattern(match))) {
                                    LOG.debug("matchNode:     FOUND, regular expression match, {}={}", levelName, match);
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
                        LOG.debug("matchNode:    NOT FOUND, nothing for {}={}", levelName, searchTerm);
                        break;
                    }
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

            // Set the 'searched' attribute.
            @SuppressWarnings("NonConstantStringShouldBeStringBuffer")
            String searched = "";
            for (final String levelName : root.getLevels()) {
                final String searchTerm = nullToEmpty(levelSearchTerms.get(levelName));
                searched = searched + (searched.isEmpty() ? "" : "&") + levelName + '=' + searchTerm;
            }
            searchResult.setSearched(searched);

            // Set the 'matched' of the node from which the parameters were gotten.
            final String matched = getMatchedValue(0, root, nodeOfParameters, "").getValue1();
            searchResult.setMatched(matched);
            results.add(searchResult);
            LOG.debug("matchNode:   searched={}, matched={}", searched, matched);
        }
        final SearchResultsDTO searchResults = new SearchResultsDTO(results);
        return searchResults;
    }

    /**
     * Given a full node path, return the node and its parent node, or null.
     * <p>
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
                    final String name = notNullOr(child.getMatch(), "");
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
     * Get matched search terms. Note that the node object itself will be searched for, so the method
     * will use an object equality test to find a specific node, not an equals() test.
     *
     * @param level      Number of level at which we are searching.
     * @param tree       Tree to search the node in.
     * @param node       Node to search for.
     * @param pathPrefix Path to be used as prefix (without trailing '/').
     *                   This is supplied to be able to make the method recursively callable.
     * @return A tuple with as value 1 the matched search terms, within the specified tree and as value 2 a boolean
     * which indicates whether the node was found or not. If not, the returned path equals the path prefix.
     */
    @Nonnull
    private Tuple<String, Boolean> getMatchedValue(
            final int level,
            @Nonnull final Node tree,
            @Nullable final Node node,
            @Nonnull final String pathPrefix) {
        if (tree.getNodes() != null) {
            for (final Node child : tree.getNodes()) {

                // Get level name.
                assert root.getLevels() != null;
                assert level < root.getLevels().size();
                @SuppressWarnings("ConstantConditions")
                final String levelName = root.getLevels().get(level);

                // Get match string from node.
                final String nodeMatch = notNullOr(child.getMatch(), "");

                // Check if this is the exact node (object equality).
                //noinspection ObjectEquality
                if (child == node) {

                    // Get level name and append search term.
                    return new Tuple<>(pathPrefix + (pathPrefix.isEmpty() ? "" : "&") + levelName + '=' + nodeMatch, true);
                } else {
                    final Tuple<String, Boolean> found = getMatchedValue(level + 1, child, node,
                            pathPrefix + (pathPrefix.isEmpty() ? "" : "&") + levelName + '=' + nodeMatch);
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
    private static <T> T getChildObjectFromConfiguration(
            @Nonnull final String content,
            @Nonnull final JavaType type)
            throws IncorrectConfigurationException {
        // Read tree as JSON or XML.
        try {
            // Try to read as JSON first.
            final ObjectMapper mapper = new ObjectMapper();
            mapper.configure(Feature.ALLOW_COMMENTS, true);
            return mapper.readValue(content, type);
        } catch (final IOException e1) {
            try {
                // If JSON fails, try XML instead.
                final XmlMapper mapper = new XmlMapper();
                return mapper.readValue(content, type);
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
    }

    @Nonnull
    private static NodeDTO getRootNodeAndValidateTree(
            @Nonnull final String include,
            @Nonnull final String content)
            throws IncorrectConfigurationException {

        // Read the tree from the configuration.
        final NodeDTO rawTree = getChildObjectFromConfiguration(content, constructType(NodeDTO.class));

        // Inline all includes recursively.
        final List<NodeDTO> rootTree = expandAllIncludes(rawTree, new ArrayList<>(), NodeDTO.class);
        if (rootTree.size() != 1) {
            throw new IncorrectConfigurationException("Configuration is not OK! Root should contain a single node.");
        }
        final NodeDTO root = rootTree.get(0);

        // Check if node match strings do not conflict.
        if (!checkNodeMatchStringsRoot(root)) {
            throw new IncorrectConfigurationException("Configuration is not OK! " +
                    "Nodes match strings are incorrectly formatted, not unique or contain incorrect key/value pairs.");
        }

        // Validate the (sub)tree.
        root.validate();

        LOG.debug("getRootNodeAndValidateTree: Configuration for node '{}' is OK", include);

        // Check if the match string of the root is null; all child match strings have been checked by now (when tree was read in).
        if (root.getMatch() != null) {
            throw new IncorrectConfigurationException("Configuration is not OK! Top-level root node must not contain a match string.");
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
                if (!isValidMatchString(level)) {
                    throw new IncorrectConfigurationException("Level name cannot contain '" + SEPARATOR_WRONG +
                            "', '" + SEPARATOR_PATH + "' or '" + SEPARATOR_QUERY + "'.");
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

    private static boolean isValidMatchString(@Nonnull final String match) {
        return ((match.indexOf(SEPARATOR_WRONG) + match.indexOf(SEPARATOR_PATH) + match.indexOf(SEPARATOR_QUERY)) == -3);
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
     * Give a series of nodes at the same level, check if the match strings are OK.
     * Check the children as well
     *
     * @param root Root node.
     * @return True if all OK, false otherwise.
     */
    private static boolean checkNodeMatchStringsRoot(@Nonnull final NodeDTO root) {
        return checkNodeMatchStringsChildren(root.getNodes());
    }

    /**
     * Give a series of nodes at the same level, check if the match strings are OK.
     * Check the children as well
     *
     * @param children Set of nodes.
     * @return True if all OK, false otherwise.
     */
    private static boolean checkNodeMatchStringsChildren(@Nullable final List<NodeDTO> children) {
        if (children == null) {
            return true;
        }
        boolean ok = true;
        final Set<String> matches = new HashSet<>();      // Node match strings (per level).
        for (final NodeDTO child : children) {
            final String match = child.getMatch();
            if (match == null) {
                ok = false;
                LOG.error("checkNodeMatchStringsChildren: match cannot be null");
            } else if (match.isEmpty()) {
                ok = false;
                LOG.error("checkNodeMatchStringsChildren: match cannot be empty");
            } else if (!isValidMatchString(match)) {
                ok = false;
                LOG.error("checkNodeMatchStringsChildren: incorrect format fort match");
            } else if (matches.contains(match)) {
                ok = false;
                LOG.error("checkNodeMatchStringsChildren: match string must be unique, match={}", match);
            } else {
                matches.add(match);
                ok = ok && checkNodeMatchStringsChildren(child.getNodes());
            }
        }
        return ok;
    }

    /**
     * Check that all parameters have a key and a value.
     *
     * @param params list of parameters to check
     * @throws IncorrectConfigurationException If a parameter was found without key or value.
     */
    private static void checkAllParamsHaveKeyAndValue(final @Nonnull List<ParameterDTO> params) throws IncorrectConfigurationException {
        for (final ParameterDTO p : params) {
            if (p.getKey() == null) {
                throw new IncorrectConfigurationException("Parameter found without key");
            }
            if (p.getValue() == null) {
                throw new IncorrectConfigurationException("Parameter found without value");
            }
        }
    }

    /**
     * Expand all the included subtrees in a thing with includes.
     *
     * @param object    Object to expand.
     * @param included Memory of which include files were processed.
     * @param clazz    Class type of which a list of these needs to be returned.
     * @return Replacements for the object that was just expanded.
     * @throws IncorrectConfigurationException If include recursion was detected.
     */
    private static <T extends SupportsInclude> List<T> expandAllIncludes(
            @Nonnull final T object,
            @Nonnull final List<String> included,
            @Nonnull final Class<T> clazz) throws IncorrectConfigurationException {
        final List<T> replacementObjects;
        final String include = object.getInclude();
        final String includeArray = object.getIncludeArray();
        if (include != null) {
            replacementObjects = getReplacementObjectsFromInclude(included, include, clazz, false);
            assert replacementObjects.size() == 1;
        } else if (includeArray != null) {
            replacementObjects = getReplacementObjectsFromInclude(included, includeArray, clazz, true);
            assert replacementObjects.size() >= 1;
        } else {
            replacementObjects = Arrays.asList(object);

            // For nodes we need to dive deeper.
            if (object instanceof NodeDTO) {
                final NodeDTO nodeDTO = (NodeDTO) object;

                // No "include" or "include_array" as found, just process children.
                final List<NodeDTO> childrenNodes = nodeDTO.getNodes();
                if (childrenNodes != null) {
                    final List<NodeDTO> replacementNodes = new ArrayList<>();
                    for (final NodeDTO childNode : childrenNodes) {
                        replacementNodes.addAll(expandAllIncludes(childNode, included, NodeDTO.class));
                    }

                    // Replace existing nodes with expanded ones.
                    childrenNodes.clear();
                    childrenNodes.addAll(replacementNodes);
                }

                // Process parameter objects as well.
                final List<ParameterDTO> parameters = nodeDTO.getParameters();
                if (parameters != null) {
                    final List<ParameterDTO> replacementParameters = new ArrayList<>();
                    for (final ParameterDTO parameter : parameters) {
                        replacementParameters.addAll(expandAllIncludes(parameter, included, ParameterDTO.class));
                    }

                    checkAllParamsHaveKeyAndValue(replacementParameters);
                    nodeDTO.setParameters(new ParameterListDTO(replacementParameters));
                }
            }
        }
        return replacementObjects;
    }

    /**
     * Replaces include statement with 0..n replacements loaded from a different file. It guarantees that no more
     * include statements are present in the returned output.
     *
     * @param included List of files included so far (for cycle detection)
     * @param include  File to include
     * @param clazz    Reference to the type of object that should be loaded from the target include.
     * @param multiple Boolean indicating whether the file should contain an array or a single object.
     * @param <T>      Type of object to load from the target file. Must equal the clazz.
     * @return List of replacement objects. The list may always contain multiple or zero returns, as any replacement
     * itself may be an include for zero or multiple objects.
     * @throws IncorrectConfigurationException If there is a detected problem with the configuration at this point.
     */
    @Nonnull
    private static <T extends SupportsInclude> List<T> getReplacementObjectsFromInclude(
            @Nonnull final List<String> included,
            @Nonnull final String include,
            @Nonnull final Class<T> clazz,
            final boolean multiple) throws IncorrectConfigurationException {

        // Check for endless recursion.
        if (included.contains(include)) {
            throw new IncorrectConfigurationException("Endless recursion detected at include=" + include);
        }

        // Push name to stack.
        included.add(0, include);

        // Read JSON content from include.
        final String content = readConfiguration(include);

        // Parse nodes from content.
        final List<T> childrenNotExpanded;
        if (multiple) {

            // Was: include_array.
            childrenNotExpanded = getChildObjectFromConfiguration(content, constructCollectionType(List.class, clazz));
        } else {

            // Was: include.
            final T child = getChildObjectFromConfiguration(content, constructType(clazz));
            childrenNotExpanded = Arrays.asList(child);
        }

        // Expand all includes in children as well and construct final list of children.
        final List<T> children = new ArrayList<>();
        for (final T childNotExpanded : childrenNotExpanded) {
            children.addAll(expandAllIncludes(childNotExpanded, included, clazz));
        }

        // Pop name from stack.
        final String removed = included.remove(0);
        assert removed.equals(include);
        return children;
    }

    @Nonnull
    private static JavaType constructType(@Nonnull Class<?> clazz) {
        return TypeFactory.defaultInstance().constructType(clazz);
    }

    @SuppressWarnings("rawtypes")
    @Nonnull
    private static JavaType constructCollectionType(@Nonnull Class<? extends Collection> collection, @Nonnull Class<?> clazz) {
        return TypeFactory.defaultInstance().constructCollectionType(collection, clazz);
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
