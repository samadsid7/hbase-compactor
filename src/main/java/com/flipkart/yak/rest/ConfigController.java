package com.flipkart.yak.rest;

import com.flipkart.yak.config.*;
import com.flipkart.yak.config.loader.AbstractConfigLoader;
import com.flipkart.yak.config.loader.AbstractConfigWriter;
import com.flipkart.yak.interfaces.Factory;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.configuration.ConfigurationException;
import org.apache.commons.lang.time.DateUtils;

import javax.ws.rs.*;
import javax.ws.rs.core.MediaType;
import java.time.Instant;
import java.time.LocalDateTime;
import java.util.List;

import static com.flipkart.yak.commons.ScheduleUtils.getStartOfTheDay;

/**
 * Controller class for all REST APIs.
 * Takes in {@link Factory} as parameter, and uses to obtain DAOs to store/retrieve data in/from Store.
 */
@Slf4j
@Path("/manage")
public class ConfigController {
    private final AbstractConfigWriter abstractConfigWriter;
    private final AbstractConfigLoader abstractConfigLoader;
    private final Object storeResource;

    public ConfigController(Factory storeFactory, String storeResourceProperty) throws ConfigurationException {
         abstractConfigWriter = storeFactory.getWriter();
         abstractConfigLoader = storeFactory.getLoader();
         storeResource = abstractConfigWriter.init(storeResourceProperty);
    }

    @POST
    @Path("/context")
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    public boolean addContext(CompactionContext compactionContext) {
        boolean response = abstractConfigWriter.storeContext(storeResource, compactionContext);
        return response;
    }

    @DELETE
    @Path("/context")
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    public boolean removeContext(CompactionContext compactionContext) {
        boolean response = abstractConfigWriter.deleteContext(storeResource, compactionContext);
        return response;
    }

    @POST
    @Path("/trigger")
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    public boolean triggerImmediateCompaction(CompactionContext compactionContext) {
        compactionContext.getCompactionSchedule().setPrompt(true);
        long lifeCycleStartTime = (long)(Instant.now().toEpochMilli());
        long lifeCycleEndTime = getStartOfTheDay(Instant.now()) + (long)(compactionContext.getCompactionSchedule().getEndHourOfTheDay() * DateUtils.MILLIS_PER_HOUR);
        compactionContext.getCompactionSchedule().setCompactionScheduleLifeCycle(new CompactionScheduleLifeCycle(lifeCycleStartTime, lifeCycleEndTime));
        boolean response = abstractConfigWriter.storeContext(storeResource, compactionContext);
        return response;
    }

    @DELETE
    @Path("/deleteStaleContexts")
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    public boolean deleteStaleConfig() {
        boolean response = abstractConfigWriter.deleteStaleContexts(storeResource);
        return response;
    }

    @POST
    @Path("/profile")
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    public boolean addProfile(CompactionProfileConfig compactionProfileConfig) {
        boolean response = abstractConfigWriter.storeProfile(storeResource, compactionProfileConfig);
        return response;
    }

    @GET
    @Path("/contexts")
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    public List<CompactionContext> getContexts() {
        try {
            return abstractConfigLoader.getCompactionContexts(storeResource);
        } catch (ConfigurationException e) {
            throw new RuntimeException(e);
        }
    }

    @GET
    @Path("/profiles")
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    public List<CompactionProfileConfig> getProfiles() {
        try {
            return abstractConfigLoader.getProfiles(storeResource);
        } catch (ConfigurationException e) {
            throw new RuntimeException(e);
        }
    }
}
