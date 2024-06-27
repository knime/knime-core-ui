package org.knime.core.webui.node.dialog.defaultdialog.widget.dynamic;

import org.knime.core.webui.node.dialog.defaultdialog.DefaultNodeSettings;
import org.knime.core.webui.node.dialog.defaultdialog.widget.Label;
import org.knime.core.webui.node.dialog.defaultdialog.widget.dynamic.DynamicValuesInput.ModifiersRegistry;

public final class StringValueModifiers implements DefaultNodeSettings {

    static {
        ModifiersRegistry.modifierClasses.put(StringValueModifiers.class.getName(), StringValueModifiers.class);
    }

    public enum CaseMatching {
            /** Respect case when matching strings. */
            @Label("Case sensitive")
            CASESENSITIVE, //
            /** Disregard case when matching strings. */
            @Label("Case insensitive")
            CASEINSENSITIVE;

        /** Recommended default setting. */
        public static final CaseMatching DEFAULT = CASESENSITIVE;
    }

    CaseMatching m_caseMatching = CaseMatching.DEFAULT;

    public boolean isCaseSensitive() {
        return this.m_caseMatching == CaseMatching.CASESENSITIVE;
    }

    @Override
    public boolean equals(final Object obj) {
        if (obj == this) {
            return true;
        }
        if (obj == null) {
            return false;
        }
        if (!(obj instanceof StringValueModifiers mod)) {
            return false;
        }
        return m_caseMatching == mod.m_caseMatching;
    }

    @Override
    public int hashCode() {
        return m_caseMatching.hashCode();
    }
}
