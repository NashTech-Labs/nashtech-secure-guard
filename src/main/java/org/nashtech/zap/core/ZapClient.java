package org.nashtech.zap.core;

import org.nashtech.zap.config.ZapConfig;
import org.zaproxy.clientapi.core.ClientApi;

public class ZapClient {
    private ClientApi clientApi;

    public ZapClient(int zapPort) {
        ZapConfig config = new ZapConfig();
        String zapAddress = config.getProperty("zap.address");
        //int zapPort = Integer.parseInt(config.getProperty("zap.port"));
        String zapApiKey = config.getProperty("zap.apikey");

        clientApi = new ClientApi(zapAddress, zapPort, zapApiKey);
    }

    public ClientApi getClientApi() {
        return clientApi;
    }
}
