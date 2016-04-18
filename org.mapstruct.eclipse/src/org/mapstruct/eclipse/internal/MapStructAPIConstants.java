/**
 *  Copyright 2012-2015 Gunnar Morling (http://www.gunnarmorling.de/)
 *  and/or other contributors as indicated by the @authors tag. See the
 *  copyright.txt file in the distribution for a full listing of all
 *  contributors.
 *
 *  Licensed under the Apache License, Version 2.0 (the "License");
 *  you may not use this file except in compliance with the License.
 *  You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  See the License for the specific language governing permissions and
 *  limitations under the License.
 */
package org.mapstruct.eclipse.internal;

/**
 * Constants for types from the MapStruct API
 *
 * @author Andreas Gudian
 */
public final class MapStructAPIConstants {
    private MapStructAPIConstants() {
    }

    /**
     * MapStruct package
     */
    private static final String ORG_MAPSTRUCT = "org.mapstruct."; //$NON-NLS-1$

    /**
     * Simple name of the annotation Mapper
     */
    public static final String MAPPER_SIMPLE_NAME = "Mapper"; //$NON-NLS-1$
    /**
     * Fully qualified name of the annotation Mapper
     */
    public static final String MAPPER_FQ_NAME = ORG_MAPSTRUCT + MAPPER_SIMPLE_NAME;

    /**
     * Simple name of the annotation Mapping
     */
    public static final String MAPPING_SIMPLE_NAME = "Mapping"; //$NON-NLS-1$

    /**
     * Simple name of the annotation ValueMapping
     */
    public static final String VALUE_MAPPING_SIMPLE_NAME = "ValueMapping"; //$NON-NLS-1$

    /**
     * Fully qualified name of the annotation Mapping
     */
    public static final String MAPPING_FQ_NAME = ORG_MAPSTRUCT + MAPPING_SIMPLE_NAME;

    /**
     * Fully qualified name of the annotation ValueMapping
     */
    public static final String VALUE_MAPPING_FQ_NAME = ORG_MAPSTRUCT + VALUE_MAPPING_SIMPLE_NAME;

    /**
     * Simple name of the annotation Mappings
     */
    public static final String MAPPINGS_SIMPLE_NAME = "Mappings"; //$NON-NLS-1$

    /**
     * Fully qualified name of the annotation Mappings
     */
    public static final String MAPPINGS_FQ_NAME = ORG_MAPSTRUCT + MAPPINGS_SIMPLE_NAME;

    /**
     * Member name of Mapping#source()
     */
    public static final String MAPPING_MEMBER_SOURCE = "source"; //$NON-NLS-1$

    /**
     * Member name of Mapping#target()
     */
    public static final String MAPPING_MEMBER_TARGET = "target"; //$NON-NLS-1$

    /**
     * Member name of Mapping#ignore()
     */
    public static final String MAPPING_MEMBER_IGNORE = "ignore"; //$NON-NLS-1$

    /**
     * Fully qualified name of the annotation TargetType
     */
    public static final String TARGET_TYPE_FQ_NAME = "org.mapstruct.TargetType"; //$NON-NLS-1$

    /**
     * Fully qualified name of the annotation MappingTarget
     */
    public static final String MAPPING_TARGET_FQ_NAME = "org.mapstruct.MappingTarget"; //$NON-NLS-1$
}
