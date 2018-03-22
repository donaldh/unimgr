/*
 * Copyright (c) 2014 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.opendaylight.legato;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import org.opendaylight.legato.util.LegatoConstants;

public class TEST {

	/**
	 * @param args
	 */
	public static void main(String[] args) {
		// TODO Auto-generated method stub

		String uni1 = "12345";
		String uni2 = "45678";
		String uni3 = "23456";
		String uni4 = "56789";
		
		List<Map<String, Object>> evcListBefore = new ArrayList<Map<String, Object>> ();
		List<Map<String, Object>> evcListAfter = new ArrayList<Map<String, Object>> ();
		
		List<String> uniList1 = new ArrayList<String>();
		uniList1.add(uni1);
		uniList1.add(uni2);
		
		List<String> uniList2 = new ArrayList<String>();
		uniList2.add(uni1);
		uniList2.add(uni2);
		//uniList2.add(uni3);
		
		List<String> uniList3 = new ArrayList<String>();
		uniList3.add(uni2);
		uniList3.add(uni1);
		
		Map<String, Object> parsedEvcBeforeMap1 = new HashMap<String, Object>();
		
		parsedEvcBeforeMap1.put(LegatoConstants.EVC_ID, "EVC1");
		parsedEvcBeforeMap1.put(LegatoConstants.EVC_MAX_FRAME, 1000);
		parsedEvcBeforeMap1.put(LegatoConstants.EVC_STATUS, "") ;
		parsedEvcBeforeMap1.put(LegatoConstants.EVC_CON_TYPE, "P2P");
		parsedEvcBeforeMap1.put(LegatoConstants.EVC_UNI_LIST, uniList3);
		
		evcListBefore.add(parsedEvcBeforeMap1);
		
		Map<String, Object> parsedEvcBeforeMap2 = new HashMap<String, Object>();
		
		parsedEvcBeforeMap2.put(LegatoConstants.EVC_ID, "EVC2");
		parsedEvcBeforeMap2.put(LegatoConstants.EVC_MAX_FRAME, 2000);
		parsedEvcBeforeMap2.put(LegatoConstants.EVC_STATUS, "") ;
		parsedEvcBeforeMap2.put(LegatoConstants.EVC_CON_TYPE, "P2P");
		parsedEvcBeforeMap2.put(LegatoConstants.EVC_UNI_LIST, uniList1);
		
		evcListBefore.add(parsedEvcBeforeMap2);
		
		Map<String, Object> parsedEvcAfterMap1 = new HashMap<String, Object>();
		
		parsedEvcAfterMap1.put(LegatoConstants.EVC_ID, "EVC1");
		parsedEvcAfterMap1.put(LegatoConstants.EVC_MAX_FRAME, 1000);
		parsedEvcAfterMap1.put(LegatoConstants.EVC_STATUS, "") ;
		parsedEvcAfterMap1.put(LegatoConstants.EVC_CON_TYPE, "P2P");
		parsedEvcAfterMap1.put(LegatoConstants.EVC_UNI_LIST, uniList1);
		
		evcListAfter.add(parsedEvcAfterMap1);
		
		Map<String, Object> parsedEvcAfterMap2 = new HashMap<String, Object>();
		
		parsedEvcAfterMap2.put(LegatoConstants.EVC_ID, "EVC2");
		parsedEvcAfterMap2.put(LegatoConstants.EVC_MAX_FRAME, 2000);
		parsedEvcAfterMap2.put(LegatoConstants.EVC_STATUS, "") ;
		parsedEvcAfterMap2.put(LegatoConstants.EVC_CON_TYPE, "P2P");
		parsedEvcAfterMap2.put(LegatoConstants.EVC_UNI_LIST, uniList2);
		
		evcListAfter.add(parsedEvcAfterMap2);
		
		/*if(parsedEvcAfterMap2.equals(parsedEvcBeforeMap1))
			System.out.println(" MATCH");
		else
			System.out.println(" DOES NOT MATCH");*/
		
		System.out.println("Before : " + evcListBefore.toString());

		System.out.println("After  : " + evcListAfter.toString());
		
		System.out.println(evcListBefore.stream().collect(Collectors.toList()));
		
		if(evcListBefore.equals(evcListAfter))
			System.out.println(" MATCH");
		else
			System.out.println(" DOES NOT MATCH");
		
		//System.out.println(evcListBefore.remove(evcListAfter));
		
	}

}
