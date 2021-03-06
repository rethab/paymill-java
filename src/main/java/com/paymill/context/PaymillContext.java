package com.paymill.context;

import java.io.IOException;
import java.io.InputStream;
import java.lang.reflect.Constructor;
import java.util.Date;
import java.util.Properties;

import com.paymill.utils.HttpClient;
import com.paymill.utils.JerseyClient;
import org.apache.commons.beanutils.ConvertUtils;
import org.apache.commons.beanutils.converters.DateConverter;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.paymill.models.Deserializer;
import com.paymill.services.ChecksumService;
import com.paymill.services.ClientService;
import com.paymill.services.OfferService;
import com.paymill.services.PaymentService;
import com.paymill.services.PreauthorizationService;
import com.paymill.services.RefundService;
import com.paymill.services.SubscriptionService;
import com.paymill.services.TransactionService;
import com.paymill.services.WebhookService;

/*

PPPPPPPPPPPPPPPPP        AAA           YYYYYYY       YYYYYYYMMMMMMMM               MMMMMMMMIIIIIIIIIILLLLLLLLLLL             LLLLLLLLLLL
P::::::::::::::::P      A:::A          Y:::::Y       Y:::::YM:::::::M             M:::::::MI::::::::IL:::::::::L             L:::::::::L
P::::::PPPPPP:::::P    A:::::A         Y:::::Y       Y:::::YM::::::::M           M::::::::MI::::::::IL:::::::::L             L:::::::::L
PP:::::P     P:::::P  A:::::::A        Y::::::Y     Y::::::YM:::::::::M         M:::::::::MII::::::IILL:::::::LL             LL:::::::LL
  P::::P     P:::::P A:::::::::A       YYY:::::Y   Y:::::YYYM::::::::::M       M::::::::::M  I::::I    L:::::L                 L:::::L                           cccccccccccccccc   ooooooooooo      mmmmmmm    mmmmmmm
  P::::P     P:::::PA:::::A:::::A         Y:::::Y Y:::::Y   M:::::::::::M     M:::::::::::M  I::::I    L:::::L                 L:::::L                         cc:::::::::::::::c oo:::::::::::oo  mm:::::::m  m:::::::mm
  P::::PPPPPP:::::PA:::::A A:::::A         Y:::::Y:::::Y    M:::::::M::::M   M::::M:::::::M  I::::I    L:::::L                 L:::::L                        c:::::::::::::::::co:::::::::::::::om::::::::::mm::::::::::m
  P:::::::::::::PPA:::::A   A:::::A         Y:::::::::Y     M::::::M M::::M M::::M M::::::M  I::::I    L:::::L                 L:::::L                       c:::::::cccccc:::::co:::::ooooo:::::om::::::::::::::::::::::m
  P::::PPPPPPPPP A:::::A     A:::::A         Y:::::::Y      M::::::M  M::::M::::M  M::::::M  I::::I    L:::::L                 L:::::L                       c::::::c     ccccccco::::o     o::::om:::::mmm::::::mmm:::::m
  P::::P        A:::::AAAAAAAAA:::::A         Y:::::Y       M::::::M   M:::::::M   M::::::M  I::::I    L:::::L                 L:::::L                       c:::::c             o::::o     o::::om::::m   m::::m   m::::m
  P::::P       A:::::::::::::::::::::A        Y:::::Y       M::::::M    M:::::M    M::::::M  I::::I    L:::::L                 L:::::L                       c:::::c             o::::o     o::::om::::m   m::::m   m::::m
  P::::P      A:::::AAAAAAAAAAAAA:::::A       Y:::::Y       M::::::M     MMMMM     M::::::M  I::::I    L:::::L         LLLLLL  L:::::L         LLLLLL        c::::::c     ccccccco::::o     o::::om::::m   m::::m   m::::m
PP::::::PP   A:::::A             A:::::A      Y:::::Y       M::::::M               M::::::MII::::::IILL:::::::LLLLLLLLL:::::LLL:::::::LLLLLLLLL:::::L        c:::::::cccccc:::::co:::::ooooo:::::om::::m   m::::m   m::::m
P::::::::P  A:::::A               A:::::A  YYYY:::::YYYY    M::::::M               M::::::MI::::::::IL::::::::::::::::::::::LL::::::::::::::::::::::L ......  c:::::::::::::::::co:::::::::::::::om::::m   m::::m   m::::m
P::::::::P A:::::A                 A:::::A Y:::::::::::Y    M::::::M               M::::::MI::::::::IL::::::::::::::::::::::LL::::::::::::::::::::::L .::::.   cc:::::::::::::::c oo:::::::::::oo m::::m   m::::m   m::::m
PPPPPPPPPPAAAAAAA                   AAAAAAAYYYYYYYYYYYYY    MMMMMMMM               MMMMMMMMIIIIIIIIIILLLLLLLLLLLLLLLLLLLLLLLLLLLLLLLLLLLLLLLLLLLLLLLL ......     cccccccccccccccc   ooooooooooo   mmmmmm   mmmmmm   mmmmmm

Become part of the team
http://jobs.paymill.com

*/




/**
 * PaymillContecxt loads the context of PAYMILL for a single account, by providing a merchants private key <br>
 * It creates 8 services, which represents the PAYMILL API:
 * <ul>
 * <li>{@link ChecksumService}</li>
 * <li>{@link ClientService}</li>
 * <li>{@link OfferService}</li>
 * <li>{@link PaymentService}</li>
 * <li>{@link PreauthorizationService}</li>
 * <li>{@link RefundService}</li>
 * <li>{@link SubscriptionService}</li>
 * <li>{@link TransactionService}</li>
 * <li>{@link WebhookService}</li>
 * </ul>
 * This services should not be created directly. They have to be obtained by the context's accessors.
 * @author Vassil Nikolov
 * @since 3.0.0
 */
public class PaymillContext {

  public final static ObjectMapper PARSER     = new ObjectMapper();
  private final static Properties  PROPERTIES = new Properties();

  private final HttpClient         httpClient;

  private ChecksumService          checksumService;
  private ClientService            clientService;
  private OfferService             offerService;
  private PaymentService           paymentService;
  private PreauthorizationService  preauthorizationService;
  private RefundService            refundService;
  private SubscriptionService      subscriptionService;
  private TransactionService       transactionService;
  private WebhookService           webhookService;

  static {
    PARSER.registerModule( Deserializer.getDeserializerModule() );
  }

  /**
   * Creates a PAYMILL context with the given apiKey. Connection timeout to PAYMILL by default is set to infinity.
   * @param apiKey
   *          Private key from PAYMILL merchant center.
   */
  public PaymillContext( final String apiKey ) {
    this( apiKey, null );
  }

  /**
   * Creates a PAYMILL context with the given apiKey and timeout.
   * @param apiKey
   *          Private key from PAYMILL merchant center.
   * @param timeout
   *          Timeout in milliseconds for the HTTP connection to PAYMILL. If <code>null</code> or <code>0</code> then an interval
   *          of infinity is declared.
   */
  public PaymillContext( final String apiKey, Integer timeout ) {
    this( new JerseyClient( apiKey, timeout ) );
  }

  /**
   * Creates a PAYMILL context with the given HttpClient implementation.
   * @param client
   *          Http client implementation.
   */
  public PaymillContext( final HttpClient client ) {
    ConvertUtils.register( new DateConverter( null ), Date.class );
    InputStream input = null;

    try {
      this.httpClient = client;

      this.checksumService = this.getPrivateConstructor( ChecksumService.class ).newInstance( this.httpClient );
      this.clientService = this.getPrivateConstructor( ClientService.class ).newInstance( this.httpClient );
      this.offerService = this.getPrivateConstructor( OfferService.class ).newInstance( this.httpClient );
      this.paymentService = this.getPrivateConstructor( PaymentService.class ).newInstance( this.httpClient );
      this.preauthorizationService = this.getPrivateConstructor( PreauthorizationService.class ).newInstance( this.httpClient );
      this.refundService = this.getPrivateConstructor( RefundService.class ).newInstance( this.httpClient );
      this.subscriptionService = this.getPrivateConstructor( SubscriptionService.class ).newInstance( this.httpClient );
      this.transactionService = this.getPrivateConstructor( TransactionService.class ).newInstance( this.httpClient );
      this.webhookService = this.getPrivateConstructor( WebhookService.class ).newInstance( this.httpClient );

      input = PaymillContext.class.getClassLoader().getResourceAsStream( "META-INF/maven/com.paymill/paymill-java/pom.properties" );
      PaymillContext.PROPERTIES.load( input );

    } catch( Exception exc ) {
      throw new RuntimeException( exc );
    } finally {
      if( input != null ) {
        try {
          input.close();
        } catch( IOException e ) {
          e.printStackTrace();
        }
      }
    }
  }

  public final static String getProjectName() {
    return PaymillContext.PROPERTIES.getProperty( "artifactId" );
  }

  public final static String getProjectVersion() {
    return PaymillContext.PROPERTIES.getProperty( "version" );
  }

  public ChecksumService getChecksumService() {
    return this.checksumService;
  }

  public ClientService getClientService() {
    return this.clientService;
  }

  public OfferService getOfferService() {
    return this.offerService;
  }

  public PaymentService getPaymentService() {
    return this.paymentService;
  }

  public PreauthorizationService getPreauthorizationService() {
    return this.preauthorizationService;
  }

  public RefundService getRefundService() {
    return this.refundService;
  }

  public SubscriptionService getSubscriptionService() {
    return this.subscriptionService;
  }

  public TransactionService getTransactionService() {
    return this.transactionService;
  }

  public WebhookService getWebhookService() {
    return this.webhookService;
  }

  private <T> Constructor<T> getPrivateConstructor( final Class<T> clazz ) throws Exception {
    Constructor<T> declaredConstructor = clazz.getDeclaredConstructor( HttpClient.class );
    declaredConstructor.setAccessible( true );
    return declaredConstructor;
  }

}
