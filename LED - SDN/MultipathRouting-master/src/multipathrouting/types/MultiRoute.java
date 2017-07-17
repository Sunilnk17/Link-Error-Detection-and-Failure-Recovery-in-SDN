package net.floodlightcontroller.multipathrouting;

import java.util.ArrayList;

import net.floodlightcontroller.routing.Route;

public class MultiRoute {
    protected int routeCount;
    protected int routeSize;
    protected ArrayList<Route> routes;

    public MultiRoute(){
        routeCount = 0;
        routeSize = 0;
        routes = new ArrayList<Route>();
    }
    
    public Route getRoute(){
        routeCount = (routeCount+1)%routeSize;
        return routes.get(routeCount);
    }

    public int getRouteCount(){
        return routeCount;
    }
    public int getRouteSize(){
        return routeSize;    
    }
    public void addRoute(Route route){
        routeSize++;
        routes.add(route);
    }
}
