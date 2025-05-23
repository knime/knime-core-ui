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
 *   Jun 21, 2023 (Paul Bärnreuther): created
 */
package org.knime.core.webui.node.dialog.defaultdialog.util;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.lang.reflect.ParameterizedType;
import java.util.List;

import org.junit.jupiter.api.Test;

/**
 *
 * @author Paul Bärnreuther
 */
@SuppressWarnings("java:S2698") // we accept assertions without messages
class GenericTypeFinderUtilTest {
    @SuppressWarnings("unused")
    static interface GenericInterface<A, B, C> {
    }

    static Class<?> getNthTypeInterface(@SuppressWarnings("rawtypes") final Class<? extends GenericInterface> clazz,
        final int n) {
        return (Class<?>)GenericTypeFinderUtil.getNthGenericType(clazz, GenericInterface.class, n);
    }

    @Test
    void testFindsGenericTypesOfInterface() {

        class SimpleImplementation implements GenericInterface<String, Integer, Boolean> {

        }
        final var first = getNthTypeInterface(SimpleImplementation.class, 0);
        final var second = getNthTypeInterface(SimpleImplementation.class, 1);
        final var third = getNthTypeInterface(SimpleImplementation.class, 2);

        assertThrows(IndexOutOfBoundsException.class, () -> getNthTypeInterface(SimpleImplementation.class, 3));
        assertThrows(IndexOutOfBoundsException.class, () -> getNthTypeInterface(SimpleImplementation.class, -1));

        assertThat(first).isEqualTo(String.class);
        assertThat(second).isEqualTo(Integer.class);
        assertThat(third).isEqualTo(Boolean.class);
    }

    @Test
    void testFindsGenericTypesForCorrectInterface() {

        @SuppressWarnings("unused")
        interface GenericInterface2<A, B, C> {
        }

        interface NonGenericInterface {
        }

        class MultiImplementation implements GenericInterface2<Long, Long, Long>, NonGenericInterface,
            GenericInterface<String, Integer, Boolean> {

        }
        final var first = getNthTypeInterface(MultiImplementation.class, 0);
        final var second = getNthTypeInterface(MultiImplementation.class, 1);
        final var third = getNthTypeInterface(MultiImplementation.class, 2);

        assertThat(first).isEqualTo(String.class);
        assertThat(second).isEqualTo(Integer.class);
        assertThat(third).isEqualTo(Boolean.class);
    }

    @Test
    void testFindsGenericTypesForSuperInterface() {

        @SuppressWarnings("unused")
        interface SubInterface<S> extends GenericInterface<String, S, Boolean> {
        }

        @SuppressWarnings("unused")
        interface SubSubInterface extends SubInterface<Integer> {
        }

        class DeepImplementation implements SubSubInterface {
        }

        final var first = getNthTypeInterface(DeepImplementation.class, 0);
        final var second = getNthTypeInterface(DeepImplementation.class, 1);
        final var third = getNthTypeInterface(DeepImplementation.class, 2);

        assertThat(first).isEqualTo(String.class);
        assertThat(second).isEqualTo(Integer.class);
        assertThat(third).isEqualTo(Boolean.class);
    }

    @Test
    void testFindsGenericTypesOfInterfaceForDescendants() {

        class Descendant1<A, B> implements GenericInterface<A, B, Boolean> {
        }
        class Descendant2<A> extends Descendant1<A, Integer> {
        }
        class Descendant3 extends Descendant2<String> {
        }
        class Descendant4 extends Descendant3 {
        }

        final var first = getNthTypeInterface(Descendant4.class, 0);
        final var second = getNthTypeInterface(Descendant4.class, 1);
        final var third = getNthTypeInterface(Descendant4.class, 2);

        assertThat(first).isEqualTo(String.class);
        assertThat(second).isEqualTo(Integer.class);
        assertThat(third).isEqualTo(Boolean.class);

        assertThat(getNthTypeInterface(Descendant1.class, 2)).isEqualTo(Boolean.class);
        assertThrows(ClassCastException.class, () -> getNthTypeInterface(Descendant1.class, 1));
    }

    @SuppressWarnings("unused")
    abstract class GenericSuperClass<A, B, C> {
    }

    static Class<?> getNthTypeClass(@SuppressWarnings("rawtypes") final Class<? extends GenericSuperClass> clazz,
        final int n) {
        return (Class<?>)GenericTypeFinderUtil.getNthGenericType(clazz, GenericSuperClass.class, n);
    }

    @Test
    void testFindsGenericTypesOfSuperClass() {

        class SimpleImplementation extends GenericSuperClass<String, Integer, Boolean> {

        }
        final var first = getNthTypeClass(SimpleImplementation.class, 0);
        final var second = getNthTypeClass(SimpleImplementation.class, 1);
        final var third = getNthTypeClass(SimpleImplementation.class, 2);

        assertThrows(IndexOutOfBoundsException.class, () -> getNthTypeClass(SimpleImplementation.class, 3));
        assertThrows(IndexOutOfBoundsException.class, () -> getNthTypeClass(SimpleImplementation.class, -1));

        assertThat(first).isEqualTo(String.class);
        assertThat(second).isEqualTo(Integer.class);
        assertThat(third).isEqualTo(Boolean.class);
    }

    @Test
    void testFindsGenericParameterizedTypes() {

        class SimpleImplementation extends GenericSuperClass<List<String>, Integer, Boolean> {

        }
        final var first =
            GenericTypeFinderUtil.getFirstGenericType(SimpleImplementation.class, GenericSuperClass.class);

        assertThat(first).isInstanceOf(ParameterizedType.class);
        assertThat(((ParameterizedType)first).getRawType()).isEqualTo(List.class);
    }

    @Test
    void testFindsGenericTypesOfAbstractClassDescendants() {

        class Descendant1<A, B> extends GenericSuperClass<A, B, Boolean> {
        }
        class Descendant2<A> extends Descendant1<A, Integer> {
        }
        class Descendant3 extends Descendant2<String> {
        }
        class Descendant4 extends Descendant3 {
        }

        final var first = getNthTypeClass(Descendant4.class, 0);
        final var second = getNthTypeClass(Descendant4.class, 1);
        final var third = getNthTypeClass(Descendant4.class, 2);

        assertThat(first).isEqualTo(String.class);
        assertThat(second).isEqualTo(Integer.class);
        assertThat(third).isEqualTo(Boolean.class);

        assertThat(getNthTypeClass(Descendant1.class, 2)).isEqualTo(Boolean.class);
        assertThrows(ClassCastException.class, () -> getNthTypeClass(Descendant1.class, 1));
    }

    @SuppressWarnings("unused")
    interface GenericInterface2<A> {
    }

    static final class GenericClass<B> implements GenericInterface2<B> {
    }

    @SuppressWarnings("unused")
    static class AnotherGenericClass<C> {
    }

    static final class TestClass extends AnotherGenericClass<GenericClass<String>> {

    }

    @Test
    void testFindGenericTypedFromParameterizedTypes() {

        final var parameterizedType =
            GenericTypeFinderUtil.getFirstGenericTypeFromType(TestClass.class, AnotherGenericClass.class);

        final var genericTypeFromGenericClass =
            GenericTypeFinderUtil.getFirstGenericTypeFromType(parameterizedType, GenericClass.class);

        final var genericTypeFromGenericInterface =
            GenericTypeFinderUtil.getFirstGenericTypeFromType(parameterizedType, GenericInterface2.class);

        assertThat(genericTypeFromGenericClass).isEqualTo(String.class);
        assertThat(genericTypeFromGenericInterface).isEqualTo(String.class);

    }

}
