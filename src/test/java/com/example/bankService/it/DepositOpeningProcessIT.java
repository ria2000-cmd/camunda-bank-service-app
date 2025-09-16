package com.example.bankService.it;

import com.example.bankService.model.Client;
import lombok.AccessLevel;
import lombok.experimental.FieldDefaults;
import org.camunda.bpm.engine.ManagementService;
import org.camunda.bpm.engine.RuntimeService;
import org.camunda.bpm.engine.TaskService;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;

import java.math.BigDecimal;
import java.util.HashMap;
import java.util.Map;

import static com.example.bankService.util.Constants.*;
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


      //start process BankEmailCongrats
      var bankEmailCongrats = runtimeService.createProcessInstanceQuery()
              .processDefinitionKey("BankEmailCongratsId")
              .active()
              .singleResult();
      assertThat(bankEmailCongrats).isNotNull();

      assertThat(bankEmailCongrats).hasPassed("SignalStartEmailCongratsId");

      assertThat(bankEmailCongrats).isWaitingAt("EmailDelayTimerId");

     var emailDelayTimerJob =  managementService.createJobQuery()
                      .timers()
                      .activityId("EmailDelayTimerId")
                      .processInstanceId(bankEmailCongrats.getProcessInstanceId())
                     .singleResult();
      assertThat(emailDelayTimerJob).isNotNull();

      managementService.executeJob(emailDelayTimerJob.getId());

      assertThat(bankEmailCongrats).hasPassed("EmailDelayTimerId", "ActivityCongratsEmailAfterwardsId", "CongratsEndEvent");
      assertThat(bankEmailCongrats).isEnded();

      //start process BankSmsCongrats
      var bankSmsCongrats = runtimeService.createProcessInstanceQuery()
              .processDefinitionKey("BankSmsCongratsId")
              .active()
              .singleResult();
      assertThat(bankSmsCongrats).isNotNull();

      assertThat(bankSmsCongrats).hasPassed("SignalStartSmsCongratsId");

      assertThat(bankSmsCongrats).isWaitingAt("SmsDelayTimerId");

      var smsDelayTimerJob =  managementService.createJobQuery()
              .timers()
              .activityId("SmsDelayTimerId")
              .processInstanceId(bankSmsCongrats.getProcessInstanceId())
              .singleResult();
      assertThat(smsDelayTimerJob).isNotNull();

      managementService.executeJob(smsDelayTimerJob.getId());

      assertThat(bankSmsCongrats).hasPassed("smsDelayTimerJob",
                                                     "ActivityCongratsSmsAfterwardsId",
                                                     "SmsCongratsEndEventId");
      assertThat(bankSmsCongrats).isEnded();

  }




    @Test
    void depositEndToEndTest_shouldCallToThePolice(){

        var variables = prepareVariables(RENDY);


        //      start MainDepositCreditProcess process
        var mainDepositCreditProcess = runtimeService.startProcessInstanceByKey("MainDepositCreditProcess",
                "testBusinessKey"
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
        userTaskVariables.put("transportMode", "walk");
        userTaskVariables.put("taxiCost", "0");

        assertThat(mainDepositCreditProcess).hasNotPassed("PayForTheTaxi");

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
        assertThat(mainDepositCreditProcess).hasPassed("GetTicketInQueueMachineId");


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
        assertThat(openDeposit).hasPassed("GatewayIsDepositChosen",
                                                     "Gateway_1b1dio2",
                                                    "ClientExistingCheckingId",
                                                    "ClientFullValidationId",
                                                    "GatewayMergeIsNewClientId",
                                                    "GatewayIsSuccessValidationId",
                                                    "GatewayIsClientCriminalId",
                                                    "CallThePoliceId",
                                                    "ClientIsCriminalErrorId"

        );

        assertThat(openDeposit).hasNotPassed("StartVerificationSmsDelegate", "SignalToFinishDepositProcessId");
        assertThat(openDeposit).isWaitingAt("EndVerificationSmsDelegateId");

        assertThat(openDeposit).isEnded();

        assertThat(mainDepositCreditProcess).hasPassed("ClientIsCriminalStartErrorId",
                                                                  "ActivityRunOutOfTheBankId",
                                                                  "ClientIsCriminalEndErrorId");

        assertThat(mainDepositCreditProcess).isEnded();

    }

    @DisplayName("should throw an exception when client rejects to sign the contract more than available number of deposits" +
            "(The client hesitated and changed his mind to sign the contract" +
            "and decided to see again what are the contributions)")
    @Test
    void depositEndToEndTest_shouldInvokeAllTasks_whenContractIsNotSigned(){

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
        readAndSignContractUserTaskVariables.put("isContractSigned", "false");

        taskService.complete(readAndSignContract.getId(), readAndSignContractUserTaskVariables);
        assertThat(openDeposit).hasPassed("ReadAndSignContractId", "Gateway_0hz02db");
        assertThat(openDeposit).hasNotPassed("DepositReplenishmentId");

        assertThat(openDeposit).isWaitingAt("DepositChoosingId");

     //second traversing of task
        //start DepositChoosingId User task
        var secondUserTaskDepositChoosing = taskService.createTaskQuery()
                .processInstanceId(openDeposit.getProcessInstanceId())
                .taskName("Take a look in to deposit list and choose one of them")
                .taskDefinitionKey("DepositChoosingId")
                .singleResult();

        assertThat(secondUserTaskDepositChoosing).isNotNull();

        var secondUserTaskDepositChoosingVariables = new HashMap<String, Object>();
        secondUserTaskDepositChoosingVariables.put("depositName", "Hot-Summer");


        taskService.complete(secondUserTaskDepositChoosing.getId(), secondUserTaskDepositChoosingVariables);

        assertThat(openDeposit).hasPassed("DepositChoosingId", "GatewayIsDepositChosenId", "Gateway_1b1dio2");

        //Link checking
        assertThat(openDeposit).isWaitingAt("DepositChoosingCountEndLinkId");
        execute(job("DepositChoosingCountEndLinkId"));

        assertThat(openDeposit).hasPassed( "DocumentsPreparationId");

        assertThat(openDeposit).isWaitingAt("ReadAndSignContractId");


        //start secondReadAndSignContract User task
        var secondReadAndSignContract= taskService.createTaskQuery()
                .processInstanceId(openDeposit.getProcessInstanceId())
                .taskName("Read and sign contract")
                .taskDefinitionKey("ReadAndSignContractId")
                .singleResult();

        assertThat(secondReadAndSignContract).isNotNull();

        var secondReadAndSignContractUserTaskVariables = new HashMap<String, Object>();
        secondReadAndSignContractUserTaskVariables.put("isContractSigned", "false");

        taskService.complete(secondReadAndSignContract.getId(), secondReadAndSignContractUserTaskVariables);
        assertThat(openDeposit).hasPassed("ReadAndSignContractId", "Gateway_0hz02db");
        assertThat(openDeposit).hasNotPassed("DepositReplenishmentId");

        assertThat(openDeposit).isWaitingAt("DepositChoosingId");


        //third traversing of task
        //start DepositChoosingId User task
        var thirdUserTaskDepositChoosing = taskService.createTaskQuery()
                .processInstanceId(openDeposit.getProcessInstanceId())
                .taskName("Take a look in to deposit list and choose one of them")
                .taskDefinitionKey("DepositChoosingId")
                .singleResult();

        assertThat(thirdUserTaskDepositChoosing).isNotNull();

        var  thirdUserTaskDepositChoosingVariables = new HashMap<String, Object>();
        thirdUserTaskDepositChoosingVariables.put("depositName", "Hello-Winter");


        taskService.complete(thirdUserTaskDepositChoosing.getId(), thirdUserTaskDepositChoosingVariables);

        assertThat(openDeposit).hasPassed("DepositChoosingId", "GatewayIsDepositChosenId", "Gateway_1b1dio2");

        //Link checking
        assertThat(openDeposit).isWaitingAt("DepositChoosingCountEndLinkId");
        execute(job("DepositChoosingCountEndLinkId"));

        assertThat(openDeposit).hasPassed("DepositChoosingCountEndLinkId", "DocumentsPreparationId");

        assertThat(openDeposit).isWaitingAt("ReadAndSignContractId");


        //start thirdReadAndSignContract User task
        var thirdReadAndSignContract= taskService.createTaskQuery()
                .processInstanceId(openDeposit.getProcessInstanceId())
                .taskName("Read and sign contract")
                .taskDefinitionKey("ReadAndSignContractId")
                .singleResult();

        assertThat(secondReadAndSignContract).isNotNull();

        var thirdReadAndSignContractUserTaskVariables = new HashMap<String, Object>();
        thirdReadAndSignContractUserTaskVariables.put("isContractSigned", "false");

//        should throw an exception
        taskService.complete(thirdReadAndSignContract.getId(), thirdReadAndSignContractUserTaskVariables);

        assertThat(openDeposit).hasPassed("ErrorNoMoreDepositHandleId", "ErrorNoMoreDepositThrowId");
        assertThat(openDeposit).isEnded();

        //continue main process
        assertThat(mainDepositCreditProcess).hasPassed("Event_1xuu5t4");
        assertThat(mainDepositCreditProcess).isWaitingAt("ActivityRoadToHomeAfterNoMoreDeposits");


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

        assertThat(mainDepositCreditProcess).hasPassed("EventSuddenOperationInterruptionErrorWhenNoMoreDepositsEndId");
        assertThat(mainDepositCreditProcess).isEnded();

    }

@DisplayName(
        "should throw an exception and finish entire process when client does not have enough money on wallet " +
                "(to deposit since the deposit has minimal required amount)"
)
    @Test
    void depositEndToEndTest_whenClientDoesNotHaveEnoughMoney_shouldThrowAnExceptionAndFinish(){

        var variables = prepareVariables(DAKIE); //Dakie has only 20  bucks on her wallet

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
        assertThat(mainDepositCreditProcess).hasPassed("GoingToBankId")
                .variables()
                .extracting("client")
                .extracting("wallet")
                .extracting("moneyCount")
                .isEqualTo(BigDecimal.valueOf(4.7)); //money on the wallet after paying for the taxi


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

        //should throw an exception in DepositReplenishmentId
         assertThat(openDeposit).hasPassed("ErrorNotEnoughMoneyHandle1");
         assertThat(openDeposit).hasPassed("ErrorNotEnoughMoneyId1")
                 .variables()
                 .extracting("client")
                 .extracting("wallet")
                 .extracting("moneyCount")
                 .isEqualTo(BigDecimal.valueOf(4.7));

         assertThat(openDeposit).isEnded();
         assertThat(mainDepositCreditProcess).hasPassed("EventNotEnoughMoneyErrorStartId");
         assertThat(mainDepositCreditProcess).isWaitingAt("RoadToHomeAfterNotEnoughMoneyExceptionCallActivityId");

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

    assertThat(mainDepositCreditProcess).hasPassed("EventSuddenOperationInterruptionErrorWhenNotEnoughMoneyId");
    assertThat(mainDepositCreditProcess).isEnded();

}

  @DisplayName("User only have 3 chances to put in the code after the third time it should throw an exception")
   @Test
    void smsVerification_shouldInvokeTasks3TimesAndThrowBpmException_whenSmsIsNotValid(){

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
      //1st smsProviding userTask
      var provideSmsCodeUserTask= taskService.createTaskQuery()
              .taskName("Provide sms validation code")
              .taskDefinitionKey("ProvideSmsValidationCodeId")
              .singleResult();

      assertThat(provideSmsCodeUserTask).isNotNull();

      var mobileCode = runtimeService.getVariable(smsVerification.getId(), "sendMobileCode");
      var notValidSmsCode = "124348";

      var provideSmsCodeUserTaskVariables = new HashMap<String, Object>();
      provideSmsCodeUserTaskVariables.put("sendMobileCode", mobileCode);
      provideSmsCodeUserTaskVariables.put("obtainedMobileCode", notValidSmsCode);

      taskService.complete(provideSmsCodeUserTask.getId(), provideSmsCodeUserTaskVariables);

      assertThat(smsVerification).hasPassed("ProvideSmsValidationCodeId", "ValidateCodeFromSmsId", "GatewayIsSmsCodeValidId");
      assertThat(smsVerification).hasNotPassed("Flow_0d7utu9");

      assertThat(smsVerification).isWaitingAt("ProvideSmsValidationCodeId")
              .variables()
              .extracting("sendMobileCodeCount")
              .isEqualTo(2);



      //2nd smsProviding userTask
      var secondProvideSmsCodeUserTask= taskService.createTaskQuery()
              .taskName("Provide sms validation code")
              .taskDefinitionKey("ProvideSmsValidationCodeId")
              .singleResult();

      assertThat(secondProvideSmsCodeUserTask).isNotNull();

      taskService.complete(secondProvideSmsCodeUserTask.getId(), provideSmsCodeUserTaskVariables);

      assertThat(smsVerification).hasPassed("ProvideSmsValidationCodeId", "ValidateCodeFromSmsId", "GatewayIsSmsCodeValidId");
      assertThat(smsVerification).hasNotPassed("Flow_0d7utu9");

      assertThat(smsVerification).isWaitingAt("ProvideSmsValidationCodeId")
              .variables()
              .extracting("sendMobileCodeCount")
              .isEqualTo(3);




      //3rd
      // smsProviding userTask
      var thirdProvideSmsCodeUserTask= taskService.createTaskQuery()
              .taskName("Provide sms validation code")
              .taskDefinitionKey("ProvideSmsValidationCodeId")
              .singleResult();

      assertThat(thirdProvideSmsCodeUserTask).isNotNull();

      taskService.complete(thirdProvideSmsCodeUserTask.getId(), provideSmsCodeUserTaskVariables);

      assertThat(smsVerification).hasPassed("ErrorNoMoreSmsValidationAttempts");
      assertThat(smsVerification).isWaitingAt("SendFailedVerificationSmsId");
      assertThat(smsVerification).hasNotPassed("Flow_0d7utu9");

      execute(job("SendFailedVerificationSmsId"));

      //Handle exception in the  deposit process
      assertThat(openDeposit).hasPassed("StartFailedVerificationSmsMessageId",
              "Activity_0ik65of", "EndFailedVerificationSmsErrorId");

      //Handle exception in the main  process
      assertThat(mainDepositCreditProcess).hasPassed("EventSuddenOperationInterruptionErrorStartId");
      assertThat(mainDepositCreditProcess).isWaitingAt("RoadToHomeAfterExceptionId");

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
      assertThat(mainDepositCreditProcess).isEnded();

      }


    private Map<String, Object> prepareVariables(Client client){

        var variableMap = new HashMap<String, Object>();
        variableMap.put("client", client);
        variableMap.put("correlationId", "testCorrelationId");

        return variableMap;
    }
}
