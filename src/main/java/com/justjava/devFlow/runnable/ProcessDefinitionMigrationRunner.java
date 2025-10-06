package com.justjava.devFlow.runnable;

import org.flowable.engine.ProcessMigrationService;
import org.flowable.engine.RepositoryService;
import org.flowable.engine.RuntimeService;
import org.flowable.engine.repository.ProcessDefinition;
import org.flowable.engine.migration.ProcessInstanceMigrationBuilder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
public class ProcessDefinitionMigrationRunner implements CommandLineRunner {

    private static final Logger logger = LoggerFactory.getLogger(ProcessDefinitionMigrationRunner.class);

    private final RepositoryService repositoryService;
    private final RuntimeService runtimeService;
    private final ProcessMigrationService processMigrationService;

    public ProcessDefinitionMigrationRunner(
            RepositoryService repositoryService,
            RuntimeService runtimeService,
            ProcessMigrationService processMigrationService) {
        this.repositoryService = repositoryService;
        this.runtimeService = runtimeService;
        this.processMigrationService = processMigrationService;
    }

    @Override
    public void run(String... args) {
        logger.info("Starting process definition migration check for Flowable 7.1.0...");

        try {
            // Get all process definition keys
            List<String> processDefinitionKeys = repositoryService.createProcessDefinitionQuery()
                    .list()
                    .stream()
                    .map(ProcessDefinition::getKey)
                    .distinct()
                    .toList();

            for (String processDefinitionKey : processDefinitionKeys) {
                checkAndMigrateProcessInstances(processDefinitionKey);
            }

            logger.info("Process definition migration check completed successfully.");
        } catch (Exception e) {
            logger.error("Failed to complete process definition migration check", e);
        }
    }

    private void checkAndMigrateProcessInstances(String processDefinitionKey) {
        try {
            // Get the latest process definition
            ProcessDefinition latestDefinition = repositoryService
                    .createProcessDefinitionQuery()
                    .processDefinitionKey(processDefinitionKey)
                    .latestVersion()
                    .singleResult();

            if (latestDefinition == null) {
                logger.debug("No process definition found for key: {}", processDefinitionKey);
                return;
            }

            // Get all older process definitions for this key
            List<ProcessDefinition> olderDefinitions = repositoryService.createProcessDefinitionQuery()
                    .processDefinitionKey(processDefinitionKey)
                    .list()
                    .stream()
                    .filter(pd -> pd.getVersion() < latestDefinition.getVersion())
                    .toList();

            if (olderDefinitions.isEmpty()) {
                logger.info("No older versions found for process definition '{}'. Skipping migration.", processDefinitionKey);
                return;
            }

            logger.info("Found {} older versions for process definition '{}'. Latest is version {}",
                    olderDefinitions.size(), processDefinitionKey, latestDefinition.getVersion());

            // Migrate process instances from all older definitions
            int totalMigrated = 0;
            for (ProcessDefinition oldDefinition : olderDefinitions) {
                totalMigrated += migrateProcessInstances(oldDefinition, latestDefinition);
            }

            logger.info("Completed migration for '{}': {} instances migrated to version {}",
                    processDefinitionKey, totalMigrated, latestDefinition.getVersion());

        } catch (Exception e) {
            logger.error("Failed to migrate process instances for key: {}", processDefinitionKey, e);
        }
    }

    private int migrateProcessInstances(ProcessDefinition sourceDefinition, ProcessDefinition targetDefinition) {
        try {
            // Find running instances for the old process definition
            List<org.flowable.engine.runtime.ProcessInstance> runningInstances =
                    runtimeService.createProcessInstanceQuery()
                            .processDefinitionId(sourceDefinition.getId())
                            .list();

            if (runningInstances.isEmpty()) {
                logger.debug("No running instances found for process definition '{}' version {}.",
                        sourceDefinition.getKey(), sourceDefinition.getVersion());
                return 0;
            }

            logger.info("Found {} running instances for process definition '{}' version {}",
                    runningInstances.size(), sourceDefinition.getKey(), sourceDefinition.getVersion());

            int successCount = 0;

            for (org.flowable.engine.runtime.ProcessInstance instance : runningInstances) {
                if (migrateSingleInstance(instance, targetDefinition)) {
                    successCount++;
                }
            }

            logger.info("Successfully migrated {}/{} instances from version {} to version {}",
                    successCount, runningInstances.size(),
                    sourceDefinition.getVersion(), targetDefinition.getVersion());

            return successCount;

        } catch (Exception e) {
            logger.error("Failed to migrate process instances from version {} to version {}",
                    sourceDefinition.getVersion(), targetDefinition.getVersion(), e);
            return 0;
        }
    }

    private boolean migrateSingleInstance(org.flowable.engine.runtime.ProcessInstance instance,
                                          ProcessDefinition targetDefinition) {
        try {
            // CORRECT: Use ProcessMigrationService instead of RuntimeService
            ProcessInstanceMigrationBuilder migrationBuilder =
                    processMigrationService.createProcessInstanceMigrationBuilder()
                            .migrateToProcessDefinition(targetDefinition.getId());

            // Execute migration directly (validation is often done during migration execution in Flowable 7.x)
            migrationBuilder.migrate(instance.getId());

            logger.debug("Successfully migrated process instance {} to process definition version {}",
                    instance.getId(), targetDefinition.getVersion());

            return true;

        } catch (Exception e) {
            logger.error("Failed to migrate process instance {}: {}", instance.getId(), e.getMessage());
            return false;
        }
    }
}
