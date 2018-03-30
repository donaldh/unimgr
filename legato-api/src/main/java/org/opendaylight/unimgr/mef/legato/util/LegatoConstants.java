/*
 * Copyright (c) 2018 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.unimgr.mef.legato.util;

import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.inet.types.rev130715.Uri;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.TopologyId;

/**
 * @author santanu.de@xoriant.com
 */

public class LegatoConstants {
    
    public static final String EVC_ID = "EVC_ID";

    public static final String EVC_MAX_FRAME = "EVC_MAX_FRAME";

    public static final String EVC_STATUS = "EVC_STATUS";

    public static final String EVC_UNI_LIST = "EVC_UNI_LIST";

    public static final String EVC_CON_TYPE = "EVC_CON_TYPE";

    public static final String BEST_EFFORT = "BEST_EFFORT";

    public static final long LONG_VAL = 9223372;

    public static final String SLS_PROFILES = "SLS_PROFILES";

    public static final String COS_PROFILES = "COS_PROFILES";

    public static final String BWP_PROFILES = "BWP_PROFILES";

    public static final String POINTTOPOINT = "POINTTOPOINT";

    public static final String MULTIPOINTTOMULTIPOINT = "MULTIPOINTTOMULTIPOINT";

    public static final String l2CP_EEC_PROFILES = "l2CP_EEC_PROFILES";

    public static final String L2CP_PEERING_PROFILES = "L2CP_PEERING_PROFILES";

    public static final String EEC_PROFILES = "EEC_PROFILES";

    public static final String CMP_PROFILES = "CMP_PROFILES";

    public static final TopologyId OVSDB_TOPOLOGY_ID = new TopologyId(new Uri("ovsdb:1"));

}
