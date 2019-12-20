import org.apache.camel.BindToRegistry;
import org.apache.camel.CamelContext;
import org.apache.camel.RoutesBuilder;
import org.apache.camel.builder.RouteBuilder;

import org.apache.camel.component.http.HttpComponent;
import org.apache.camel.component.netty.http.NettyHttpComponent;
import org.apache.camel.component.netty.http.RestNettyHttpBinding;
import org.apache.camel.impl.DefaultCamelContext;
import org.apache.camel.model.rest.RestBindingMode;
import org.apache.camel.model.rest.RestDefinition;
import org.apache.camel.support.SimpleRegistry;


public class Main{
    @BindToRegistry("mybinding")


    public static void main(String[] args) throws Exception {
        CamelContext context                  = new DefaultCamelContext(new SimpleRegistry());
        NettyHttpComponent nettyHttpComponent = new NettyHttpComponent();
        context.getRegistry().bind("mybinding" , RestNettyHttpBinding.class);
        context.addComponent("netty-http", nettyHttpComponent);

        context.addRoutes(new RouteBuilder() {
            @Override
            public void configure() throws Exception {
                //restConfiguration().component("netty-http").host("localhost").port(5000).enableCORS(true).endpointProperty("nettyHttpBinding", "#mybinding");
                restConfiguration().component("netty-http").host("localhost").port(5000).enableCORS(true);

                rest().produces("application/json")
                    .get("/A").route().transform().constant("[{ \"id\":\"1\", \"name\":\"Scott\" },{ \"id\":\"2\", \"name\":\"Claus\" }]").endRest()
                    .get("/A/{id}").route().transform().simple("{ \"id\":\"${header.id}\", \"name\":\"Scott\" }").endRest();
                    //.post("/users").to("mock:create")
                    //.put("/users/{id}").to("mock:update")
                    //.delete("/users/{id}").to("mock:delete");
            }
        });

        nettyHttpComponent.start();
        context.start();

    }
}
