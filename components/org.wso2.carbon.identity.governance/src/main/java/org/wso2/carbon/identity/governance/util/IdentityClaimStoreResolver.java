/*
 * Copyright (c) 2025, WSO2 LLC. (http://www.wso2.com).
 *
 * WSO2 LLC. licenses this file to you under the Apache License,
 * Version 2.0 (the "License"); you may not use this file except
 * in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */

package org.wso2.carbon.identity.governance.util;

import org.apache.commons.lang.StringUtils;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.wso2.carbon.identity.claim.metadata.mgt.ClaimMetadataException;
import org.wso2.carbon.identity.claim.metadata.mgt.ClaimMetadataManagementService;
import org.wso2.carbon.identity.claim.metadata.mgt.model.LocalClaim;
import org.wso2.carbon.identity.claim.metadata.mgt.util.ClaimConstants;
import org.wso2.carbon.identity.core.util.IdentityTenantUtil;
import org.wso2.carbon.identity.governance.internal.IdentityMgtServiceDataHolder;
import org.wso2.carbon.user.core.UserCoreConstants;
import org.wso2.carbon.user.core.UserStoreManager;
import org.wso2.carbon.user.core.util.UserCoreUtil;

import java.util.Arrays;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * Resolves where identity claims should be stored based on claim metadata configuration and user store setup.
 */
public final class IdentityClaimStoreResolver {

    private static final Log log = LogFactory.getLog(IdentityClaimStoreResolver.class);
    private static final String COMMA = ",";

    private IdentityClaimStoreResolver() {

    }

    /**
     * Determine whether the given claim needs to be persisted in the identity data store.
     *
     * @param claimUri          Claim URI under evaluation.
     * @param userStoreManager  User store manager associated with the claim operation.
     * @return {@code true} if the claim should be stored in the identity data store, {@code false} otherwise.
     * @throws UserStoreException If claim metadata cannot be retrieved.
     */
    public static boolean shouldPersistInIdentityDataStore(String claimUri, UserStoreManager userStoreManager) {

        if (StringUtils.isBlank(claimUri) || userStoreManager == null) {
            return false;
        }

        boolean isIdentityClaim = claimUri.contains(UserCoreConstants.ClaimTypeURIs.IDENTITY_CLAIM_URI_PREFIX);

        ClaimMetadataManagementService claimMetadataManagementService =
                IdentityMgtServiceDataHolder.getInstance().getClaimMetadataManagementService();
        if (claimMetadataManagementService == null) {
            if (isIdentityClaim && log.isDebugEnabled()) {
                log.debug("Claim metadata management service is not available. Defaulting to identity data store " +
                        "for claim: " + claimUri);
            }
            return isIdentityClaim;
        }

        String tenantDomain = IdentityTenantUtil.getTenantDomain(userStoreManager.getTenantId());
        LocalClaim localClaim;
        try {
            localClaim = claimMetadataManagementService.getLocalClaim(claimUri, tenantDomain);
        } catch (ClaimMetadataException e) {
            if (isIdentityClaim) {
                log.warn("Error while retrieving claim metadata for claim: " + claimUri +
                        ". Defaulting to identity data store persistence.", e);
            }
            return isIdentityClaim;
        }

        if (localClaim == null) {
            if (isIdentityClaim && log.isDebugEnabled()) {
                log.debug("Local claim metadata not found for claim: " + claimUri +
                        ". Defaulting to identity data store persistence.");
            }
            return isIdentityClaim;
        }

        Map<String, String> properties = localClaim.getClaimProperties();
        boolean managedInUserStore = Boolean.parseBoolean(properties.getOrDefault(
                ClaimConstants.MANAGED_IN_USER_STORE_PROPERTY, Boolean.FALSE.toString()));

        if (!managedInUserStore) {
            return true;
        }

        String userStoreDomain = UserCoreUtil.getDomainName(userStoreManager.getRealmConfiguration());
        if (StringUtils.isBlank(userStoreDomain)) {
            userStoreDomain = UserCoreConstants.PRIMARY_DEFAULT_DOMAIN_NAME;
        }

        String excludedUserStores = properties.get(ClaimConstants.EXCLUDED_USER_STORES_PROPERTY);
        if (StringUtils.isBlank(excludedUserStores)) {
            return false;
        }

        Set<String> excludedDomains = Arrays.stream(excludedUserStores.split(COMMA))
                .map(String::trim)
                .filter(StringUtils::isNotBlank)
                .map(String::toUpperCase)
                .collect(Collectors.toSet());

        return excludedDomains.contains(userStoreDomain.toUpperCase());
    }

    /**
     * Extract claims that should be stored in the identity data store from the provided claims map.
     *
     * @param claims           Claim map to evaluate. Claims that are selected for identity data store persistence will
     *                         be removed from this map.
     * @param userStoreManager User store manager associated with the claim operation.
     * @return Claims that should be persisted in the identity data store.
     */
    public static Map<String, String> collectClaimsForIdentityDataStore(Map<String, String> claims,
                                                                       UserStoreManager userStoreManager) {

        Map<String, String> claimsForIdentityStore = new HashMap<>();

        if (claims == null || claims.isEmpty()) {
            return claimsForIdentityStore;
        }

        Iterator<Map.Entry<String, String>> iterator = claims.entrySet().iterator();
        while (iterator.hasNext()) {
            Map.Entry<String, String> entry = iterator.next();
            String claimUri = entry.getKey();

            if (shouldPersistInIdentityDataStore(claimUri, userStoreManager)) {
                claimsForIdentityStore.put(claimUri, entry.getValue());
                iterator.remove();
            }
        }

        return claimsForIdentityStore;
    }
}
