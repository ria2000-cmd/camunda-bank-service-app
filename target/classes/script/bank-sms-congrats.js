
var ticket = execution.getVariable('ticket') || 'your service' ;

// var clientName = execution.getVariable('clientName') ;
var clientName = 'Ria';
print('Dear'+ clientName + ','
    + '\n By this SMS we want to tell you'
    +'\n thank you for visiting our bank and for obtaining you '
    + ticket + '!'
    +'\n We hope that our services have met the highest standards.'
    + '\n If you have any questions, please feel free to contact us by call '
    +'\n Best regard, \n Your Bank Team ' );