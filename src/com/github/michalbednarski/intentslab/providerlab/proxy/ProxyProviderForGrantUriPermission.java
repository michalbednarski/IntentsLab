package com.github.michalbednarski.intentslab.providerlab.proxy;

/**
 * Version of {@link ProxyProvider} that doesn't check permissions on it's own
 */
public class ProxyProviderForGrantUriPermission extends ProxyProvider {
    public static final String AUTHORITY = "intentslab.proxyprovider.forgranturipermission";

    @Override
    protected boolean shouldSkipPermissionChecks() {
        return true;
    }
}
