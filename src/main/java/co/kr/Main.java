package co.kr;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.gson.Gson;
import com.google.gson.JsonElement;
import org.apache.camel.CamelContext;
import org.apache.camel.Exchange;
import org.apache.camel.Handler;
import org.apache.camel.Processor;
import org.apache.camel.builder.RouteBuilder;
import org.apache.camel.component.jackson.JacksonDataFormat;
import org.apache.camel.component.log.LogComponent;
import org.apache.camel.component.netty.http.*;
import org.apache.camel.impl.DefaultCamelContext;
import org.apache.camel.model.dataformat.JsonDataFormat;
import org.apache.camel.model.dataformat.JsonLibrary;
import org.apache.camel.model.rest.RestBindingMode;
import org.apache.camel.support.SimpleRegistry;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.apache.commons.lang3.builder.ReflectionToStringBuilder;

import static org.apache.camel.model.dataformat.JsonLibrary.Gson;


public class Main{
    public static void main(String[] args) throws Exception {
        final Logger logger = LoggerFactory.getLogger(Main.class);

        /** auth관리를 위한 셋팅 파일 설정 */
        System.setProperty("java.security.auth.login.config", "src/main/resources/myjaas.config");

        //component 선언
        CamelContext context                  = new DefaultCamelContext(new SimpleRegistry());
        NettyHttpComponent nettyHttpComponent = new NettyHttpComponent();
        LogComponent logComponent             = new LogComponent();

        //register add
        context.getRegistry().bind("mybinding" , RestNettyHttpBinding.class);
        context.getRegistry().bind("beanTestClass" , new BeanTestClass());


        //component add
        context.addComponent("netty-http", nettyHttpComponent);
        context.addComponent("log"       , logComponent);

        /* camel context 셋팅 */
        //context.setLogMask(false);

        /** netty http setting */
        nettyHttpComponent.setSecurityConfiguration(getHttpSecurity()); //인가 셋팅

        context.addRoutes(new RouteBuilder() {
            @Override
            public void configure() throws Exception {
                //restConfiguration().component("netty-http").host("localhost").port(5000).enableCORS(true).endpointProperty("nettyHttpBinding", "#mybinding");
                restConfiguration().component("netty-http").host("localhost").port(5000).enableCORS(true).bindingMode(RestBindingMode.auto);

                rest().produces("application/json")
                        .get("/A").route().transform().constant("[{ \"id\":\"${header.id}\", \"name\":\"Scott\" },{ \"id\":\"2\", \"name\":\"Claus\" }]").endRest()
                        .get("/admin").route().transform().constant("[{ \"id\":\"${header.id}\", \"name\":\"Scott\" },{ \"id\":\"2\", \"name\":\"Claus\" }]").log("${body}").endRest()
                        .get("/admin/{id}}").route().transform().constant("[{ \"id\":\"${header.id}\", \"name\":\"Scott\" },{ \"id\":\"2\", \"name\":\"Claus\" }]").endRest()
                        .get("/guest/{id}").route().transform().simple("{ \"id\":\"${header.id}\", \"name\":\"Scott\" }").endRest()
                        .get("/public/{id}").route().transform().simple("{ \"id\":\"${header.id}\", \"name\":\"Scott\" }").endRest()
                        .get("/restrict/c").route().transform().simple("{ \"id\":\"${header.id}\", \"name\":\"Scott\" }").endRest()
                        .get("/logout").route().transform().simple("{ \"statusMsg\":\"ok\", \"statusCode\":\"200\" }").endRest()

                        .post("/D/E").consumes("application/json").type(UserPojoEx.class).to("bean:beanTestClass")   //case 3
                ;
                /*
                case 3 call 테스트용
                $.ajax({
                        url : "http://localhost:5000/D/E",
                        type: "post",
                        accept: "application/json",
                        contentType: "application/json; charset=utf-8",
                        data: JSON.stringify({ id: 1111 , name:'abcd', active: "true" }),
                        dataType: "json"
                        })
                          .done(function( msg ) {
                                            console.log( msg );
                                        });
                 */

                /*
                from("netty-http:http://0.0.0.0:{{port}}/foo?matchOnUriPrefix=true&securityConfiguration=#mySecurityConfig")
                        .to("mock:input")
                        .transform().constant("Bye World");
                        */



                   /*.to("log:server-input")
                        .to("mock:input")
                        .transform().simple("Bye ${body}")
                        .to("log:server-output");*/

                //.post("/users").to("mock:create")
                //.put("/users/{id}").to("mock:update")
                //.delete("/users/{id}").to("mock:delete");
            }
        });

        nettyHttpComponent.start();
        context.start();
    }
    public JsonDataFormat getJsonFormat(){
        JsonDataFormat jsondf = new JsonDataFormat();
        jsondf.setLibrary(JsonLibrary.Jackson);
        jsondf.setAllowUnmarshallType(true);
        jsondf.setUnmarshalType(UserPojoEx.class);
        return jsondf;
    }

    public static NettyHttpSecurityConfiguration getHttpSecurity(){
        /** netty component setting */
        NettyHttpSecurityConfiguration security = new NettyHttpSecurityConfiguration();
        security.setRealm("karaf");
        SecurityAuthenticator auth = new JAASSecurityAuthenticator();
        auth.setName("karaf");
        security.setSecurityAuthenticator(auth);

        SecurityConstraintMapping matcher = new SecurityConstraintMapping();

        /* private  */
        matcher.addInclusion("/*");           //모든 사용자 접속 가능
        matcher.addInclusion("/admin/*","admin");       // admin 밑의 url은 해당 사용자만
        matcher.addInclusion("/guest/*","admin,guest"); // guest 밑의 url은 해동 사용자만

        /* public  */
        matcher.addExclusion("/D/E");
        matcher.addExclusion("/public/*");

        security.setSecurityConstraint(matcher);
        return security;

    }
}

class UserPojoEx {
    private int id;
    private String name;
    private boolean active;

    public int getId() {
        return id;
    }
    public void setId(int id) {
        this.id = id;
    }
    public String getName() {
        return name;
    }
    public void setName(String name) {
        this.name = name;
    }
    public boolean isActive() {
        return active;
    }
    public void setActive(boolean active) {
        this.active = active;
    }

    public String toString() {
        return ReflectionToStringBuilder.toString(this);
    }
}

class LogPrintProcess implements Processor{
    final Logger logger = LoggerFactory.getLogger(LogPrintProcess.class);
    @Override
    public void process(Exchange exchange) throws Exception {
        logger.info("header {}" , exchange.getIn().getHeaders());
        logger.info("body {}" , exchange.getIn().getBody());
    }
}

class BeanTestClass{
    final Logger logger = LoggerFactory.getLogger(BeanTestClass.class);

    public void doSomeThing(Exchange ex) throws JsonProcessingException {
        UserPojoEx bodyData = ex.getIn().getBody(UserPojoEx.class);
        logger.info("Exchage ({})" ,bodyData.toString());

        /* 변경된 데이터 들이 json 형태로 내려간다. */
        bodyData.setActive(false);
        bodyData.setId(222222);
        ex.getIn().setBody(bodyData);
    }
}



