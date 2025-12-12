/*
 * ------------------------------------------------------------------------
 *
 *  Copyright by KNIME AG, Zurich, Switzerland
 *  Website: http://www.knime.com; Email: contact@knime.com
 *
 *  This program is free software; you can redistribute it and/or modify
 *  it under the terms of the GNU General Public License, Version 3, as
 *  published by the Free Software Foundation.
 *
 *  This program is distributed in the hope that it will be useful, but
 *  WITHOUT ANY WARRANTY; without even the implied warranty of
 *  MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 *  GNU General Public License for more details.
 *
 *  You should have received a copy of the GNU General Public License
 *  along with this program; if not, see <http://www.gnu.org/licenses>.
 *
 *  Additional permission under GNU GPL version 3 section 7:
 *
 *  KNIME interoperates with ECLIPSE solely via ECLIPSE's plug-in APIs.
 *  Hence, KNIME and ECLIPSE are both independent programs and are not
 *  derived from each other. Should, however, the interpretation of the
 *  GNU GPL Version 3 ("License") under any applicable laws result in
 *  KNIME and ECLIPSE being a combined program, KNIME AG herewith grants
 *  you the additional permission to use and propagate KNIME together with
 *  ECLIPSE with only the license terms in place for ECLIPSE applying to
 *  ECLIPSE and the GNU GPL Version 3 applying for KNIME, provided the
 *  license terms of ECLIPSE themselves allow for the respective use and
 *  propagation of ECLIPSE together with KNIME.
 *
 *  Additional permission relating to nodes for KNIME that extend the Node
 *  Extension (and in particular that are based on subclasses of NodeModel,
 *  NodeDialog, and NodeView) and that only interoperate with KNIME through
 *  standard APIs ("Nodes"):
 *  Nodes are deemed to be separate and independent programs and to not be
 *  covered works.  Notwithstanding anything to the contrary in the
 *  License, the License does not apply to Nodes, you are not required to
 *  license Nodes under the License, and you are granted a license to
 *  prepare and propagate Nodes, in each case even if such Nodes are
 *  propagated with or for interoperation with KNIME.  The owner of a Node
 *  may freely choose the license terms applicable to such Node, including
 *  when such Node is propagated with or for interoperation with KNIME.
 * ---------------------------------------------------------------------
 *
 * History
 *   11 Nov 2024 (Robin Gerling): created
 */
package org.knime.core.webui.node.dialog.defaultdialog.internal.dynamic.extensions.filtervalue;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Optional;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.function.Predicate;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.apache.commons.lang3.StringUtils;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IConfigurationElement;
import org.eclipse.core.runtime.IExtension;
import org.eclipse.core.runtime.Platform;
import org.knime.core.data.DataColumnSpec;
import org.knime.core.data.DataType;
import org.knime.core.data.DataTypeRegistry;
import org.knime.core.data.DataValue;
import org.knime.core.node.InvalidSettingsException;
import org.knime.core.node.NodeLogger;
import org.knime.core.node.util.CheckUtils;
import org.knime.core.webui.node.dialog.defaultdialog.internal.dynamic.extensions.filtervalue.builtin.EqualsOperator;
import org.knime.core.webui.node.dialog.defaultdialog.internal.dynamic.extensions.filtervalue.builtin.GreaterThanOperator;
import org.knime.core.webui.node.dialog.defaultdialog.internal.dynamic.extensions.filtervalue.builtin.GreaterThanOrEqualOperator;
import org.knime.core.webui.node.dialog.defaultdialog.internal.dynamic.extensions.filtervalue.builtin.IsMissingOperator;
import org.knime.core.webui.node.dialog.defaultdialog.internal.dynamic.extensions.filtervalue.builtin.IsNotMissingOperator;
import org.knime.core.webui.node.dialog.defaultdialog.internal.dynamic.extensions.filtervalue.builtin.LessThanOperator;
import org.knime.core.webui.node.dialog.defaultdialog.internal.dynamic.extensions.filtervalue.builtin.LessThanOrEqualOperator;
import org.knime.core.webui.node.dialog.defaultdialog.internal.dynamic.extensions.filtervalue.builtin.NotEqualsNorMissingOperator;
import org.knime.core.webui.node.dialog.defaultdialog.internal.dynamic.extensions.filtervalue.builtin.NotEqualsOperator;
import org.knime.core.webui.node.dialog.defaultdialog.internal.dynamic.extensions.filtervalue.builtin.RegexOperator;
import org.knime.core.webui.node.dialog.defaultdialog.internal.dynamic.extensions.filtervalue.builtin.WildcardOperator;

/**
 * <p>
 * <b>Important:</b> The filter operator extension point is currently still <b>experimental</b> and might change in
 * future versions!
 * </p>
 *
 * The registry of all available filter operators, that is built-in operators and operators provided via the
 * "org.knime.core.ui.filterOperators" extension point.
 *
 * Extensions can only register operators for their own data types, i.e. they need to be defined in the same namespace.
 *
 *
 * <h2>Built-in operators</h2>
 *
 * KNIME provides a set of built-in operators, some of which can be overwritten by extensions.
 *
 * <h3>Non-overwritable built-in operators</h3>
 *
 * The operators {@code IS_MISSING} and {@code IS_NOT_MISSING} are built-in and cannot be overwritten by extensions.
 * They are available for columns of all data types.
 *
 * <h3>Overwritable built-in operators</h3>
 *
 * <p>
 * All built-in overwritable operators listed below have a fixed ID and implement a specific interface. As their name
 * suggests, they are provided by KNIME, but can be overwritten by extensions. Overwriting means that an extension
 * implements a {@link FilterOperator} with the respective interface listed in the table.</p
 * <p>
 * <b>Note:</b> Changing the ID will lead to a runtime exception during loading of the extension.
 * </p>
 *
 * <p>
 * Usually, these built-in operators do not need to be overwritten. One benefit of overwriting is that the operator
 * could accept more runtime types and convert values appropriately.
 * </p>
 *
 * <h4>Equality operators</h4>
 * <table>
 * <tr>
 * <th>Operator</th>
 * <th>ID</th>
 * <th>Interface</th>
 * </tr>
 * <tr>
 * <td>Equals</td>
 * <td>{@code EQ}</td>
 * <td>{@link EqualsOperator}</td>
 * </tr>
 * <tr>
 * <td>Not Equals</td>
 * <td>{@code NEQ}</td>
 * <td>{@link NotEqualsOperator}</td>
 * </tr>
 * <tr>
 * <td>Not Equals nor Missing</td>
 * <td>{@code NEQ_MISS}</td>
 * <td>{@link NotEqualsNorMissingOperator}</td>
 * </tr>
 * </table>
 *
 * <em>The equality operators are based on the {@code equals} method of the data type's cell implementation.</em>
 *
 * <h4>Comparison operators</h4> For data types which implement {@code BoundedValue}, the following set of built-in
 * comparison operators is available:
 *
 * <table>
 * <tr>
 * <th>Operator</th>
 * <th>ID</th>
 * <th>Interface</th>
 * </tr>
 * <tr>
 * <td>Less Than</td>
 * <td>{@code LT}</td>
 * <td>{@link LessThanOperator}</td>
 * </tr>
 * <tr>
 * <td>Less Than or Equal</td>
 * <td>{@code LTE}</td>
 * <td>{@link LessThanOrEqualOperator}</td>
 * </tr>
 * <tr>
 * <td>Greater Than</td>
 * <td>{@code GT}</td>
 * <td>{@link GreaterThanOperator}</td>
 * </tr>
 * <tr>
 * <td>Greater Than or Equal</td>
 * <td>{@code GTE}</td>
 * <td>{@link GreaterThanOrEqualOperator}</td>
 * </tr>
 * </table>
 *
 * <em>The comparison operators are based on the {@code Comparator} of the cell implementation.</em>
 *
 *
 * <h2>Custom operators</h2>
 *
 * <p>
 * Extensions can define custom operators for their own data types via the "org.knime.core.ui.filterOperators" extension
 * point (<em>experimental</em>).
 * Custom operators need to implement the {@link FilterOperator} interface, define a stable ID, and specify a
 * class implementing the {@link FilterValueParameters} interface. The parameters class defines the user input to the
 * operator and governs which widgets are shown in the dialog.
 * </p>
 *
 * <h3>ID namespacing for extension operators</h3>
 *
 * <p>
 * To prevent ID conflicts of third-party and build-in operators, the IDs of operators contributed via the extension
 * point are automatically modified:
 * </p>
 * <ul>
 * <li>The ID is converted to uppercase</li>
 * <li>The ID is prefixed with {@code EXT_} to distinguish it from built-in operators</li>
 * </ul>
 *
 * <p>
 * For example, if an extension defines an operator with ID {@code "myOperator"}, the effective ID registered will be
 * {@code "EXT_MYOPERATOR"}.
 * </p>
 *
 * <p>
 * <b>Best Practices:</b>
 * </p>
 * <ul>
 * <li><b>Use a short unique prefix</b> in your operator IDs to avoid conflicts with other extensions (e.g.,
 * {@code "myext_contains"} becomes {@code "EXT_MYEXT_CONTAINS"})</li>
 * <li><b>Do not version your operator IDs.</b> Instead, use a new parameter class with appropriate
 * {@link org.knime.node.parameters.persistence.Persistor} or {@link org.knime.node.parameters.migration.Migration}
 * logic to handle backward compatibility</li>
 * <li><b>Keep IDs stable</b> across versions of your extension to ensure saved workflows continue to work</li>
 * </ul>
 *
 * <h2>Duplicate IDs and deprecation</h2>
 *
 * <p>
 * The built-in operators reserve certain IDs (see table above) for all data types. Custom operators must not use these
 * IDs, except if they follow the rules for overwriting these built-in operators.
 * </p>
 *
 * <p>
 * The order in which operators are registered is currently mostly undefined. KNIME operators are registered before any
 * 3rd party operators. This ensures that installing an extension does not break core KNIME functionality.
 * </p>
 *
 * <p>
 * In case an operator defines a non-built-in ID, that is already used by <em>another</em> extension's operator, this
 * operator will not be registered and instead an error will be logged. If this operator's ID is already being used by
 * another operator of the <em>same</em> extension, the ID can be re-used, iff their parameters classes are different
 * and
 * <ul>
 * <li>this operator is marked as deprecated, or</li>
 * <li>the other operator is marked as deprecated.</li>
 * </ul>
 *
 * This mechanism allows to replace an operator with a new one, while keeping the same ID. The disambiguation then
 * happens via the parameters class.
 * </p>
 *
 * @author Paul Bärnreuther, KNIME GmbH
 * @author Manuel Hotz, KNIME GmbH, Konstanz, Germany
 *
 * @apiNote The extension point is currently still <b>experimental</b> and might change in future versions.
 * @noreference This class is not intended to be referenced by clients.
 */
public final class FilterOperatorsRegistry {

    private static final NodeLogger LOGGER = NodeLogger.getLogger(FilterOperatorsRegistry.class);

    private static final String EXT_POINT_ID = "org.knime.core.ui.filterOperators";

    static final String PROP_FEATURE_FLAG_ENABLE_EXTERNAL =
        "org.knime.core.ui.filterOperators.enableExperimentalExtensionPoint";

    /**
     * Reserved IDs for overridable built-in operators that extensions can use.
     *
     * Note: All new built-in operators that can be overridden must be added here.
     */
    static final Map<String, Class<? extends FilterOperatorBase>> OVERRIDE_FILTER_OPERATOR_BASES = Map.ofEntries( //
        Map.entry(EqualsOperator.ID, EqualsOperator.class), //
        Map.entry(NotEqualsOperator.ID, NotEqualsOperator.class), //
        Map.entry(NotEqualsNorMissingOperator.ID, NotEqualsNorMissingOperator.class), //
        Map.entry(LessThanOperator.ID, LessThanOperator.class), //
        Map.entry(GreaterThanOperator.ID, GreaterThanOperator.class), //
        Map.entry(LessThanOrEqualOperator.ID, LessThanOrEqualOperator.class), //
        Map.entry(GreaterThanOrEqualOperator.ID, GreaterThanOrEqualOperator.class), //
        Map.entry(WildcardOperator.ID, WildcardOperator.class), //
        Map.entry(RegexOperator.ID, RegexOperator.class) //
    );

    /**
     * Reserved IDs for non-overwritable built-in operators that extensions cannot use.
     *
     * Note: All new built-in operators that can be overridden must be added here.
     */
    static final List<String> RESERVED_NON_OVERRIDABLE_IDS = List.of(IsMissingOperator.ID, IsNotMissingOperator.ID);

    /**
     * Prefix applied to IDs of operators contributed via the extension point.
     */
    private static final String EXTENSION_ID_PREFIX = "EXT_";

    private final Predicate<String> m_internalNamespaces;

    private final Function<DataType, Optional<String>> m_dataTypeNamespaceLookup;

    private static final FilterOperatorsRegistry INSTANCE = new FilterOperatorsRegistry();

    private final Map<DataType, FilterOperatorContribution> m_filterOperators;

    /**
     * Feature flag to enable loading of third-party extensions.
     */
    private final boolean m_enableExternalExtensions;

    // default registry behavior
    private FilterOperatorsRegistry() {
        m_internalNamespaces = ns -> ns != null && (ns.startsWith("org.knime.") || ns.startsWith("com.knime."));
        m_dataTypeNamespaceLookup = dt -> DataTypeRegistry.getInstance().getDataTypeNamespace(dt);

        m_enableExternalExtensions = Boolean.getBoolean(PROP_FEATURE_FLAG_ENABLE_EXTERNAL);
        if (!m_enableExternalExtensions) {
            LOGGER.info("""
                    Experimental Row Filter extension point "%s" is disabled for 3rd party extensions: \
                    To enable it, set the system property "%s" to "true".""".formatted(EXT_POINT_ID,
                PROP_FEATURE_FLAG_ENABLE_EXTERNAL));
        }

        m_filterOperators = loadOperatorsFromExtensions();
    }

    // constructor for testing
    FilterOperatorsRegistry(final boolean enableExternalExtensions, final Predicate<String> internalNamespacesTester,
        final Function<DataType, Optional<String>> dataTypeNamespaceLookup) {
        m_enableExternalExtensions = enableExternalExtensions;
        m_internalNamespaces = internalNamespacesTester;
        m_dataTypeNamespaceLookup = dataTypeNamespaceLookup;
        m_filterOperators = loadOperatorsFromExtensions();
    }

    /**
     * Gets the singleton instance of this registry.
     *
     * @return the singleton instance of this registry
     */
    public static FilterOperatorsRegistry getInstance() {
        return INSTANCE;
    }

    /**
     * Gets the filter operators for the given data type.
     *
     * @param type data type
     * @return the filter operators for the given data type, or an empty list if there are none
     */
    public List<FilterOperator<FilterValueParameters>> getOperators(final DataType type) {
        if (!m_filterOperators.containsKey(type)) {
            return Collections.emptyList();
        }
        return m_filterOperators.get(type).operators();
    }

    /**
     * Gets all currently registered parameter classes.
     *
     * @return all currently registered parameter classes
     */
    public List<Class<FilterValueParameters>> getAllParameterClasses() {
        return m_filterOperators.values().stream().flatMap(c -> c.operators().stream())
            .map(FilterOperator::getNodeParametersClass).distinct().toList();
    }

    private record FilterOperatorContribution(String contributor, String namespace, DataType dataType,
        List<FilterOperator<FilterValueParameters>> operators) {
        FilterOperatorContribution {
            CheckUtils.checkArgument(StringUtils.isNotBlank(contributor), "contributor must not be missing");
            CheckUtils.checkArgument(StringUtils.isNotBlank(namespace), "namespace must not be missing");
            CheckUtils.checkNotNull(dataType, "dataType must not be null");
            CheckUtils.checkNotNull(operators, "operators must not be null");
        }
    }

    private boolean isExternal(final String id) {
        return !m_internalNamespaces.test(id);
    }

    private Map<DataType, FilterOperatorContribution> loadOperatorsFromExtensions() {
        final var registry = Platform.getExtensionRegistry();
        final var point = registry.getExtensionPoint(EXT_POINT_ID);
        final var extensions = point.getExtensions();

        // Separate KNIME and third-party extensions
        final var knimeExts = Stream.of(extensions).filter(ext -> !isExternal(ext.getNamespaceIdentifier())).toList();
        final var thirdPartyExts =
            Stream.of(extensions).filter(ext -> isExternal(ext.getNamespaceIdentifier())).toList();

        Map<DataType, FilterOperatorContribution> operatorsByDataType = new HashMap<>();

        // Load KNIME extensions first
        LOGGER.debugWithFormat("Loading %d KNIME filter operator extensions registered at point %s", knimeExts.size(),
            EXT_POINT_ID);
        for (final var ext : knimeExts) {
            loadExtension(ext, operatorsByDataType);
        }

        // Load third-party extensions second
        if (m_enableExternalExtensions) {
            LOGGER.debugWithFormat("Loading %d third-party filter operator extensions registered at point %s",
                thirdPartyExts.size(), EXT_POINT_ID);
            for (final var ext : thirdPartyExts) {
                loadExtension(ext, operatorsByDataType);
            }
        } else {
            LOGGER.debug( //
                () -> "Loading of third-party filter operator extensions is not enabled, %d extensions are ignored: %s"
                    .formatted( //
                        thirdPartyExts.size(), //
                        thirdPartyExts.stream() //
                            .map(IExtension::getNamespaceIdentifier).collect(Collectors.joining(", ")))); //
        }

        return operatorsByDataType;
    }

    private void loadExtension(final IExtension ext,
        final Map<DataType, FilterOperatorContribution> operatorsByDataType) {
        loadExtension(ext, //
            /* read contribution, apply intra-contrib check */
            this::readFilterOperatorsFactory,
            /* for same data type, allow only from same namespace */
            c -> hasDifferentNamespace(c, operatorsByDataType::get),
            /* merge operators for same extension */
            c -> operatorsByDataType.merge(c.dataType(), c, this::mergeContributions));
    }

    /**
     * Reads a contribution of filter operators from the given configuration element.
     *
     * @param cfe the configuration element
     * @return the contribution, or {@code null} if there was a {@link CoreException} or it was not allowed for the data
     *         type
     */
    private FilterOperatorContribution readFilterOperatorsFactory(final IConfigurationElement cfe) {
        final var contributor = cfe.getContributor().getName();
        final FilterOperators operatorsFactory;
        try {
            operatorsFactory = (FilterOperators)cfe.createExecutableExtension("factoryClass");
        } catch (CoreException ex) {
            LOGGER.error(String.format("Could not create '%s' from extension '%s': %s",
                FilterOperators.class.getSimpleName(), contributor, ex.getMessage()), ex);
            return null;
        }

        final var dataType = operatorsFactory.getDataType();
        final var namespace = cfe.getNamespaceIdentifier();
        checkNamespaceRule(namespace, dataType, contributor, operatorsFactory.getClass().getSimpleName());

        final var operators = operatorsFactory.getOperators();
        final var isExternal = !m_internalNamespaces.test(namespace);
        final var validOperators = new ArrayList<FilterOperator<FilterValueParameters>>();
        final var knownOperatorsByID = new HashMap<String, List<FilterOperator<FilterValueParameters>>>();
        for (final var op : operators) {
            // external operators that don't override built-in operators need to be wrapped
            final var needsWrapping = isExternal && getImplementedBuiltinOperatorInterfaces(op).isEmpty();
            final var wrappedOp = needsWrapping ? wrapWithNamespacedId(op) : op;
            validateOperator(wrappedOp, contributor, knownOperatorsByID::get, valid -> {
                validOperators.add(valid);
                knownOperatorsByID.computeIfAbsent(valid.getId(), k -> new ArrayList<>()).add(valid);
            });
        }
        return new FilterOperatorContribution(contributor, namespace, dataType, validOperators);
    }

    /**
     * Checks if the given contribution's namespace is different from the already registered contribution for the same
     * data type. If so, an error is logged and {@code true} is returned.
     *
     * Special handling for KNIME extensions: KNIME extensions are allowed to register operators for any KNIME data
     * types, even if another KNIME extension has already registered operators for that data type.
     *
     * @param contrib the new contribution to check
     * @param contribByDataType function to get the already registered contribution for a data type
     * @return {@code true} if the contribution's namespace is different from the already registered one and this
     *         represents a forbidden duplicate, {@code false} otherwise
     */
    private boolean hasDifferentNamespace(final FilterOperatorContribution contrib,
        final Function<DataType, FilterOperatorContribution> contribByDataType) {
        final var dataType = contrib.dataType;
        final var existingContrib = contribByDataType.apply(dataType);
        if (existingContrib != null && !hasSameNamespace(existingContrib, contrib)) {
            // Check if both contributions are from KNIME extensions and the data type is a KNIME data type
            final var isNewContribKNIME = m_internalNamespaces.test(contrib.namespace);
            final var isExistingContribKNIME = m_internalNamespaces.test(existingContrib.namespace);
            final var dataTypeLCAPackage = getLeastCommonAncestorPackage(dataType);
            final var isDataTypeKNIME = m_internalNamespaces.test(dataTypeLCAPackage);

            // Allow KNIME extensions to register operators for KNIME data types even if another KNIME extension
            // has already registered operators for that data type
            if (isNewContribKNIME && isExistingContribKNIME && isDataTypeKNIME) {
                LOGGER.debugWithFormat(
                    "Allowing KNIME extension \"%s\" to register additional filter operators for KNIME data type "
                        + "\"%s\" already registered by KNIME extension \"%s\"",
                    contrib.contributor, dataType.getName(), existingContrib.contributor);
                return false; // Allow this registration
            }

            LOGGER.error("""
                    Extension "%s" tried to register filter operators for data type "%s", \
                    but operators for this data type have already been registered by "%s". \
                    """.formatted(contrib.contributor, dataType.getName(), existingContrib.contributor));
            return true;
        }
        return false;
    }

    /**
     * Merges two contributions by combining their operators. For KNIME extensions and KNIME data types, allows merging
     * from different namespaces. Otherwise, expects contributions from the same namespace.
     *
     * @param existingContrib the already registered contribution, or {@code null} if none
     * @param newContrib the new contribution to merge
     * @return the merged contribution
     */
    private FilterOperatorContribution mergeContributions(final FilterOperatorContribution existingContrib,
        final FilterOperatorContribution newContrib) {
        if (existingContrib == null) {
            return newContrib;
        }

        // KNIME code is allowed to merge contribs from different namespaces
        final var sameNamespace = hasSameNamespace(existingContrib, newContrib);
        final var bothKNIME =
            m_internalNamespaces.test(existingContrib.namespace) && m_internalNamespaces.test(newContrib.namespace);
        final var dataTypeLCAPackage = getLeastCommonAncestorPackage(newContrib.dataType);
        final var isDataTypeKNIME = m_internalNamespaces.test(dataTypeLCAPackage);

        if (!sameNamespace && !(bothKNIME && isDataTypeKNIME)) {
            throw new IllegalStateException(
                "Unexpected contrib from different extension than already registered for non-KNIME scenario");
        }

        final var contributor = newContrib.contributor();
        final var combined = new ArrayList<>(existingContrib.operators);
        final var knownOperatorsByID =
            combined.stream().collect(Collectors.groupingBy(FilterOperator::getId, Collectors.toList()));
        // we need to apply operator validation across contributions, otherwise we could accidentally override already
        // defined operators
        for (final var op : newContrib.operators()) {
            validateOperator(op, contributor, knownOperatorsByID::get, combined::add);
        }

        // For different namespaces (allowed for KNIME), we keep the original contributor info but note the merge
        final var mergedContributor =
            sameNamespace ? existingContrib.contributor : (existingContrib.contributor + ", " + newContrib.contributor);
        final var mergedNamespace = existingContrib.namespace; // Keep the first namespace as primary
        return new FilterOperatorContribution(mergedContributor, mergedNamespace, existingContrib.dataType, combined);
    }

    private static boolean hasSameNamespace(final FilterOperatorContribution a, final FilterOperatorContribution b) {
        return a != null && a.namespace.equals(b.namespace);
    }

    private static void loadExtension(final IExtension ext, //
        final Function<IConfigurationElement, FilterOperatorContribution> readContribution, //
        final Predicate<FilterOperatorContribution> interExtensionDuplicateCheck, //
        final Consumer<FilterOperatorContribution> sameExtensionConsumer) {
        final var lbl = ext.getLabel();
        final var name = StringUtils.isNotBlank(lbl) ? lbl : ext.getContributor().getName();
        LOGGER.debugWithFormat("Registering filter operators of extension \"%s\" for point \"%s\"", name, EXT_POINT_ID);
        final var configElms = ext.getConfigurationElements();

        for (final IConfigurationElement cfe : configElms) {
            // 1. this call already does intra-factory duplicate check
            final var contrib = readContribution.apply(cfe);
            // 2. inter-extension duplicate check
            if (contrib == null || interExtensionDuplicateCheck.test(contrib)) {
                continue;
            }
            // 3. intra-extension duplicate check
            sameExtensionConsumer.accept(contrib);
        }
    }

    private static void validateOperator(final FilterOperator<FilterValueParameters> op, final String contributor,
        final Function<String, List<FilterOperator<FilterValueParameters>>> knownOperatorsByID,
        final Consumer<FilterOperator<FilterValueParameters>> ifValid) {
        // if the operator overrides a built-in operator, check that it has the correct ID
        var valid = validateOverride(op, contributor) &&
        // if it is not an override, check that it does not use a reserved ID
            validateReservedID(op, contributor) &&
            // check for duplicate IDs within this factory (at most one non-deprecated)
            checkDuplicatesInFactory(op, knownOperatorsByID, contributor);
        if (valid) {
            ifValid.accept(op);
        }
    }

    private boolean checkNamespaceRule(final String namespace, final DataType dataType, final String contributor,
        final String operatorName) {
        if (!isAllowed(namespace, dataType)) {
            LOGGER.error(String.format("""
                    Extension "%s" is not allowed to register filter operators for data type "%s" \
                    outside the extension's namespace "%s".
                    """, contributor, dataType.getIdentifier(), namespace));
            return false;
        }
        LOGGER.debugWithFormat("Allowing filter operators '%s' from contributor \"%s\" for data type '%s'",
            operatorName, contributor, dataType.getIdentifier());
        return true;
    }

    /**
     * Validates that the given operator satisfies the override requirements, if it is an override operator.
     *
     * @param op operator potentially being an override operator
     * @param factoryName factory name registering the operator
     * @param contributor contributor name of the extension registering the operator
     * @return {@code true} if the operator is not an override operator or satisfies the override requirements,
     *         {@code false} otherwise
     */
    private static boolean validateOverride(final FilterOperator<FilterValueParameters> op, final String contributor) {
        final var implementedInterfaces = getImplementedBuiltinOperatorInterfaces(op);
        if (implementedInterfaces.isEmpty()) {
            // not an override operator
            return true;
        }
        if (implementedInterfaces.size() > 1) {
            // conflicting implementations
            LOGGER.error(() -> {
                final var interfaces = implementedInterfaces.stream().map(e -> e.getValue().getSimpleName())
                    .collect(Collectors.joining(", "));
                return """
                        Operator "%s" from "%s" overrides multiple operators, but only one is allowed: "%s".
                        """.formatted(op.getLabel(), contributor, interfaces);
            });
            return false;
        }
        final var implementedInterface = implementedInterfaces.get(0);
        final var expectedId = implementedInterface.getKey();
        final var actualId = op.getId();
        if (!expectedId.equals(actualId)) {
            LOGGER.errorWithFormat(
                "Operator \"%s\" from \"%s\" declares ID \"%s\", but overrides \"%s\" and thus must declare \"%s\"",
                op.getLabel(), contributor, actualId, implementedInterface.getValue().getSimpleName(), expectedId);
            return false;
        }
        return true;
    }

    private static List<Entry<String, Class<? extends FilterOperatorBase>>>
        getImplementedBuiltinOperatorInterfaces(final FilterOperator<FilterValueParameters> op) {
        return OVERRIDE_FILTER_OPERATOR_BASES.entrySet().stream() //
            .filter(e -> e.getValue().isInstance(op)).toList();
    }

    private static boolean validateReservedID(final FilterOperator<FilterValueParameters> op,
        final String contributor) {
        final var isOverride =
            OVERRIDE_FILTER_OPERATOR_BASES.values().stream().anyMatch(predicate -> predicate.isInstance(op));
        if (isOverride) {
            // checked by previous check
            return true;
        }

        final var operatorId = op.getId();

        // Check for non-overridable reserved IDs
        if (RESERVED_NON_OVERRIDABLE_IDS.contains(operatorId)) {
            LOGGER.errorWithFormat("Operator \"%s\" from \"%s\" declares reserved non-overridable ID \"%s\".",
                op.getLabel(), contributor, operatorId);
            return false;
        }

        // not an override operator, not allowed to use reserved ID for overwritable operators
        final var usesOverrideKey = OVERRIDE_FILTER_OPERATOR_BASES.containsKey(operatorId);
        if (!usesOverrideKey) {
            return true;
        }
        LOGGER.errorWithFormat(
            "Operator \"%s\" from \"%s\" declares reserved ID \"%s\", which is of non-override operator. "
                + "Class: %s; implemented interfaces: %s",
            op.getLabel(), contributor, operatorId, //
            op.getClass().getCanonicalName(), //
            Arrays.stream(op.getClass().getInterfaces()).map(Class::getSimpleName).collect(Collectors.joining(", ")));
        return false;
    }

    /**
     * Applies duplicate check logic for operators from the same FilterOperators factory. For operators with the same ID
     * within the same factory, they can be re-used iff their parameters classes are different and at most one of them
     * is not marked as duplicate.
     */
    private static boolean checkDuplicatesInFactory(final FilterOperator<FilterValueParameters> op,
        final Function<String, List<FilterOperator<FilterValueParameters>>> knownByID, final String contributor) {
        final var id = op.getId();
        final var known = knownByID.apply(id);
        if (known == null) {
            // no duplicate ID
            return true;
        }

        // duplicate IDs need unique params classes (to disambiguate them) and at most one of them can be non-deprecated
        final var paramClass = op.getNodeParametersClass();
        final var withUniqueParams = known.stream().filter(f -> {
            final var isValid = !paramClass.equals(f.getNodeParametersClass());
            if (!isValid) {
                LOGGER.errorWithFormat(
                    "Operator \"%s\" from \"%s\" duplicates ID \"%s\" of already registered operator \"%s\", "
                        + "but reuses the same parameters class \"%s\".",
                    op.getLabel(), contributor, id, f.getLabel(), paramClass.getSimpleName());
            }
            return isValid;
        });

        // there can be at most one non-deprecated operator with the same ID
        return withUniqueParams.allMatch(f -> {
            final var isValid = !op.isDeprecated() ^ !f.isDeprecated();
            if (!isValid) {
                LOGGER.errorWithFormat(
                        "Operator \"%s\" from \"%s\" duplicates ID \"%s\" of already registered operator \"%s\", but"
                            + " one of them is not marked as deprecated.",
                        op.getLabel(), contributor, id, f.getLabel());
            }
            return isValid;
        });
    }

    private static String getLeastCommonAncestorPackage(final DataType dataType) {
        final var cellClass = dataType.getCellClass();
        if (cellClass != null) {
            return cellClass.getPackageName();
        }
        // in case there is no cell class, we use the value types
        final var packages = dataType.getValueClasses().stream().map(Class::getPackageName).toArray(String[]::new);
        return StringUtils.getCommonPrefix(packages);
    }

    private boolean isAllowed(final String extensionNamespace, final DataType operatorDataType) {
        final var dataTypeLCAPackage = getLeastCommonAncestorPackage(operatorDataType);
        final var isExtensionKNIME = m_internalNamespaces.test(extensionNamespace);
        final var isDataTypeKNIME = m_internalNamespaces.test(dataTypeLCAPackage);
        if (isExtensionKNIME && isDataTypeKNIME) {
            return true;
        }
        // ...otherwise, we only allow to define registrations for their own data types

        // The LCA package check is too restrictive: extensions can only register operators for data types whose package
        // has the extension's namespace as prefix. For example, this would exclude "org.xy.plugin" from registering
        // operators that reside in a Java package "org.xy.types".
        return m_dataTypeNamespaceLookup.apply(operatorDataType)
            .map(dtns ->
                dtns.startsWith(extensionNamespace)).orElseGet(() -> {
                LOGGER.debugWithFormat(
                    "Could not determine defining namespace for data type '%s' "
                    + "(not registered at DataType extension point?); "
                        + "rejecting registration from extension namespace '%s'",
                    operatorDataType.getIdentifier(), extensionNamespace);
                return false;
            });
    }

    /**
     * Wraps a filter operator to apply ID namespacing for extension-contributed operators. The ID is transformed by
     * converting it to uppercase and prefixing it with "EXT_".
     *
     * @param operator the operator to wrap
     * @param contributor the name of the contributing extension
     * @return a wrapped operator with a namespaced ID
     */
    private static FilterOperator<FilterValueParameters>
        wrapWithNamespacedId(final FilterOperator<FilterValueParameters> operator) {
        final var originalId = operator.getId();
        final var namespacedId = EXTENSION_ID_PREFIX + originalId.toUpperCase(Locale.US);
        return new WrappedFilterOperator(operator, namespacedId);
    }

    /**
     * Wrapper that delegates all calls to the wrapped operator except for {@link #getId()}, which returns the
     * namespaced ID.
     * Do not wrap operators that override built-in operators with this class.
     */
    private static final class WrappedFilterOperator implements FilterOperator<FilterValueParameters> {

        private final FilterOperator<FilterValueParameters> m_delegate;

        private final String m_namespacedId;

        WrappedFilterOperator(final FilterOperator<FilterValueParameters> delegate, final String namespacedId) {
            m_delegate = delegate;
            m_namespacedId = namespacedId;
        }

        @Override
        public String getId() {
            return m_namespacedId;
        }

        @Override
        public String getLabel() {
            return m_delegate.getLabel();
        }

        @Override
        public Class<FilterValueParameters> getNodeParametersClass() {
            return m_delegate.getNodeParametersClass();
        }

        @Override
        public Predicate<DataValue> createPredicate(final DataColumnSpec runtimeColumnSpec,
            final DataType configureColumnType, final FilterValueParameters filterParameters)
            throws InvalidSettingsException {
            return m_delegate.createPredicate(runtimeColumnSpec, configureColumnType, filterParameters);
        }

        @Override
        public boolean mapMissingTo() {
            return m_delegate.mapMissingTo();
        }

        @Override
        public boolean isDeprecated() {
            return m_delegate.isDeprecated();
        }
    }
}
