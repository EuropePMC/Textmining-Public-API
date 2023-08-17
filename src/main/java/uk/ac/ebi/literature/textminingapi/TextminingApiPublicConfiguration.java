package uk.ac.ebi.literature.textminingapi;

import java.util.concurrent.TimeUnit;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.client.reactive.ReactorClientHttpConnector;
import org.springframework.web.reactive.function.client.ExchangeStrategies;
import org.springframework.web.reactive.function.client.WebClient;
import org.springframework.web.util.DefaultUriBuilderFactory;

import io.netty.channel.ChannelOption;
import io.netty.handler.timeout.ReadTimeoutHandler;
import reactor.netty.http.client.HttpClient;
import reactor.netty.transport.ProxyProvider;
import uk.ac.ebi.literature.textminingapi.utility.Utility;


@Configuration
public class TextminingApiPublicConfiguration {
    private static final Logger log = LoggerFactory.getLogger(TextminingApiPublicConfiguration.class);
	 
    @Value("${europePMC.params.baseUrl}")
	private String europePMCBaseURL;
	
	@Value("${http.proxyPort}")
	private Integer europePMCProxyPort;
	    
	@Value("${http.proxyHost}")
	private String europePMCProxyHost;
	
	@Value("${europePMC.params.readtimeout}")
    private Integer europePMCReadTimeout;
    
    @Value("${europePMC.params.connectiontimeout}")
    private Integer europePMCConnectionTimeout;
	

    @Bean(name="europePMCWebClientTemplate")
    public WebClient europePMCWebClientTemplate() {
        log.info("In WebClient, before builder.build, URL:"+europePMCBaseURL);
        //Don't apply any encoding here bacause it doesn't encode things like + and /. We do it ourselves later.
        DefaultUriBuilderFactory factory = new DefaultUriBuilderFactory(europePMCBaseURL);
        factory.setEncodingMode(DefaultUriBuilderFactory.EncodingMode.NONE);
        
        HttpClient httpClient =  HttpClient.create().followRedirect(true)
                .option(ChannelOption.CONNECT_TIMEOUT_MILLIS, europePMCConnectionTimeout)
                .doOnConnected(conn -> conn.addHandlerLast(new ReadTimeoutHandler(europePMCReadTimeout, TimeUnit.MILLISECONDS)));
            
        if ((Utility.isEmpty(europePMCProxyHost)==false) && ("NULL".equalsIgnoreCase(europePMCProxyHost)==false) 
        	&& europePMCProxyPort!= null && europePMCProxyPort.intValue() > 0) {
        	httpClient = httpClient.proxy(proxy -> proxy
                        .type(ProxyProvider.Proxy.HTTP)
                        .host(europePMCProxyHost)
                        .port(europePMCProxyPort));
        }
        
        WebClient webClient = WebClient
                .builder()
                .clientConnector(new ReactorClientHttpConnector(httpClient))
                .uriBuilderFactory(factory)
                .exchangeStrategies( //This is to increase the limit on response bytes from the default of 256KB
                        ExchangeStrategies.builder()
                                .codecs(configurer -> configurer
                                        .defaultCodecs()
                                        .maxInMemorySize(1024 * 1024))
                                .build())
                .build();
        return webClient;
    }

}
