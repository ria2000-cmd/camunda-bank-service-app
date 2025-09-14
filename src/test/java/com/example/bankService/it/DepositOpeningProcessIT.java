package com.example.bankService.it;

import com.example.bankService.model.Client;
import lombok.AccessLevel;
import lombok.experimental.FieldDefaults;
import org.camunda.bpm.engine.ManagementService;
import org.camunda.bpm.engine.RuntimeService;
import org.camunda.bpm.engine.TaskService;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;

import java.util.HashMap;
import java.util.Map;

import static com.example.bankService.util.Constants.RIA;
import static org.camunda.bpm.engine.test.assertions.bpmn.BpmnAwareTests.execute;
import static org.camunda.bpm.engine.test.assertions.bpmn.BpmnAwareTests.job;
import static org.camunda.bpm.engine.test.assertions.bpmn.BpmnAwareTests.assertThat;


@ActiveProfiles({"integration-test"})
@SpringBootTest(
        properties = {
                //Disables job execution (asynchronous tasks, timer) during tests
                //Useful because tests often don't need actual async execution , and this speed them up
                "camunda.bpm.job-execution.enabled=false",

                //Generate a unique name for each process engine instance
                //Helps avoid conflicts when test run in parallel
                "camunda.bpm.generate-unique-process-engine-name=true",

                //Generate a unique name for the process application
                //Also prevent conflicts when running multiple tests at the same time
                "camunda.bpm.generate-unique-process-application-name=true",

                //Creates a unique name for the H2 (Or other embedded DB) for each test
                //Useful for test isolation, so it changes in one test don't affect the other
                "spring.datasource.generate-unique-name=true"

        },
        webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT
)
@FieldDefaults(level = AccessLevel.PRIVATE)
public class DepositOpeningProcessIT {

    @Autowired
    RuntimeService runtimeService;

    @Autowired
    TaskService taskService;

    @Autowired
    ManagementService managementService;

    @BeforeEach
    void cleanUpProcesses(){
        runtimeService.createProcessInstanceQuery().list()
                .forEach(instance ->{
                            try{
                                runtimeService.deleteProcessInstance(instance.getId(), "Test cleanup");
                            }catch (Exception e){
                                //Ignore it
                            }

        });
    }

  @AfterEach
    void completeRemainingTasks(){
      runtimeService.suspendProcessInstanceByProcessDefinitionKey("MainDepositCreditProcess");
      runtimeService.suspendProcessInstanceByProcessDefinitionKey("DepositOpening");
      runtimeService.suspendProcessInstanceByProcessDefinitionKey("GoingHome");
  }

  @Test
  void depositEndToEndTest_shouldInvokeAllTasks(){
      var variables = prepareVariables(RIA);

//      start MainDepositCreditProcess process
     var mainDepositCreditProcess = runtimeService.startProcessInstanceByKey("MainDepositCreditProcess"
              , variables );

     assertThat(mainDepositCreditProcess).isNotNull();
     assertThat(mainDepositCreditProcess).isWaitingAt("GoingToBankId");



      //start GoingToBankId User task
       var goingToBank = taskService.createTaskQuery()
              .taskName("Going to the bank")
              .taskDefinitionKey("GoingToBankId")
               .singleResult();

      assertThat(goingToBank).isNotNull();

      var userTaskVariables = new HashMap<String, Object>();
      userTaskVariables.put("transportMode", "taxi");
      userTaskVariables.put("taxiCost", "15.50");

      taskService.complete(goingToBank.getId(), userTaskVariables);
      assertThat(mainDepositCreditProcess).hasPassed("GoingToBankId");


      //start GetTicketInQueueMachine User task
      assertThat(mainDepositCreditProcess).isWaitingAt("GetTicketInQueueMachineId");
      assertThat(mainDepositCreditProcess).hasPassed("Gateway_1ocns3p");

      var getTicketInQueueMachine = taskService.createTaskQuery()
              .taskName("GetTicketInQueueMachine")
              .taskDefinitionKey("GetTicketInQueueMachineId")
              .singleResult();

      assertThat(getTicketInQueueMachine).isNotNull();

      var getTicketInQueueMachineVariables = new HashMap<String, Object>();
      getTicketInQueueMachineVariables.put("ticket", "deposit");


      taskService.complete(getTicketInQueueMachine.getId(), getTicketInQueueMachineVariables);
      assertThat(mainDepositCreditProcess).hasPassed("PayForTheTaxi", "GetTicketInQueueMachineId");


      //search for running call activity "OpenDeposit"
      var openDeposit = runtimeService.createProcessInstanceQuery()
              .processDefinitionKey("DepositOpening")
              .variableValueEquals("correlationId", "testCorrelationId")
              .active()
              .singleResult();
      assertThat(openDeposit).isNotNull();
      assertThat(openDeposit).isWaitingAt("PassportProvidingId");
      execute(job("PassportProvidingId"));

      assertThat(openDeposit).hasPassed("PassportProvidingId", "DepositListProvidingId");
      assertThat(openDeposit).isWaitingAt("DepositChoosingId");



      //start DepositChoosingId User task
      var userTaskDepositChoosing = taskService.createTaskQuery()
              .processInstanceId(openDeposit.getProcessInstanceId())
              .taskName("Take a look in to deposit list and choose one of them")
              .taskDefinitionKey("DepositChoosingId")
              .singleResult();

      assertThat(userTaskDepositChoosing).isNotNull();

      var userTaskDepositChoosingVariables = new HashMap<String, Object>();
      userTaskDepositChoosingVariables.put("depositName", "EARLY-SPRING");


      taskService.complete(userTaskDepositChoosing.getId(), userTaskDepositChoosingVariables);
      assertThat(openDeposit).hasPassed("GatewayIsDepositChosen", "Gateway_1b1dio2",
              "ClientExistingCheckingId", "ClientParticularValidationId", "GatewayMergeIsNewClientId",
              "GatewayIsSuccessValidationId", "StartVerificationSmsDelegateId"
      );

      assertThat(openDeposit).isWaitingAt("EndVerificationSmsDelegateId");


      //start SmsVerification process
      var smsVerification = runtimeService.createProcessInstanceQuery()
              .processDefinitionKey("SmsVerification")
              .processInstanceBusinessKey("depositOpeningBusinessKey")
              .variableValueEquals("correlationId", "testCorrelationId")
              .active()
              .singleResult();
      assertThat(smsVerification).isNotNull();

      assertThat(smsVerification).hasPassed("VerificationSmsStartMessageId",
              "PrepareAndSendVerificationSmsId",
              "VerificationSmsHandlingId"
      );

      assertThat(smsVerification).isWaitingAt("ProvideSmsValidationCodeId");


      //start provideSmsCodeUserTask User task
      var provideSmsCodeUserTask= taskService.createTaskQuery()
              .taskName("Provide sms validation code")
              .taskDefinitionKey("ProvideSmsValidationCodeId")
              .singleResult();

      assertThat(provideSmsCodeUserTask).isNotNull();

      var mobileCode = runtimeService.getVariable(smsVerification.getId(), "sendMobileCode");

      var provideSmsCodeUserTaskVariables = new HashMap<String, Object>();
      provideSmsCodeUserTaskVariables.put("sendMobileCode", mobileCode);
      provideSmsCodeUserTaskVariables.put("obtainedMobileCode", mobileCode);


      taskService.complete(provideSmsCodeUserTask.getId(), provideSmsCodeUserTaskVariables);


      assertThat(smsVerification).hasPassed("ProvideSmsValidationCodeId",
              "ValidateCodeFromSmsId", "Gateway_0nf9mn4","SendSuccessVerificationSmsId");
      assertThat(smsVerification).isWaitingAt("SendSuccessVerificationSmsId");

      execute(job("SendSuccessVerificationSmsId"));

//      Continue deposit opening process
      assertThat(openDeposit).hasPassed("EndVerificationSmsDelegateId", "DocumentsPreparationId");
      assertThat(openDeposit).isWaitingAt("ReadAndSignContractId");



      //start ReadAndSignContract User task
      var readAndSignContract= taskService.createTaskQuery()
              .processInstanceId(openDeposit.getProcessInstanceId())
              .taskName("Read and sign contract")
              .taskDefinitionKey("ReadAndSignContractId")
              .singleResult();

      assertThat(readAndSignContract).isNotNull();

      var readAndSignContractUserTaskVariables = new HashMap<String, Object>();
      readAndSignContractUserTaskVariables.put("isContractSigned", "true");

      taskService.complete(readAndSignContract.getId(), readAndSignContractUserTaskVariables);
      assertThat(openDeposit).hasPassed("ReadAndSignContractId", "Gateway_0hz02db", "DepositReplenishmentId");

      assertThat(openDeposit).isWaitingAt("CountOfMoneyToReplenishId");


      //start MoneyToReplenish User task
      var moneyToReplenish= taskService.createTaskQuery()
              .processInstanceId(openDeposit.getProcessInstanceId())
              .taskName("Choose how much money you want to replenish")
              .taskDefinitionKey("CountOfMoneyToReplenishId")
              .singleResult();

      assertThat(moneyToReplenish).isNotNull();

      var moneyToReplenishUserTaskVariables = new HashMap<String, Object>();
      moneyToReplenishUserTaskVariables.put("paidMoney", "25.0");

      taskService.complete(moneyToReplenish.getId(), moneyToReplenishUserTaskVariables);
      assertThat(openDeposit).hasPassed("CountOfMoneyToReplenishId", "MoneyCountVerificationId", "SignalToFinishDepositProcessId");

      //finish deposit opening process
      assertThat(openDeposit).isEnded();


      //start call activity RoadToHome

      var goingHomeProcess = runtimeService.createProcessInstanceQuery()
              .processDefinitionKey("GoingHome")
              .variableValueEquals("correlationId", "testCorrelationId")
              .active()
              .singleResult();
      assertThat(goingHomeProcess).isNotNull();

      assertThat(goingHomeProcess).isWaitingAt("ChooseTransportToHomeId");
      execute(job("ChooseTransportToHomeId"));

      assertThat(goingHomeProcess).hasPassed("ChooseTransportToHomeId",
              "GoingHomeProcessPrint");

      assertThat(goingHomeProcess).isEnded();


  }

    private Map<String, Object> prepareVariables(Client client){

        var variableMap = new HashMap<String, Object>();
        variableMap.put("client", client);
        variableMap.put("correlationId", "testCorrelationId");

        return variableMap;
    }
}
