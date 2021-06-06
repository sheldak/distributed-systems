const Ice = require("ice").Ice;
const Demo = require("./negator").Demo;

(async () => {
    let communicator;
    try {
        communicator = Ice.initialize(process.argv);

        const proxyA = communicator.stringToProxy("shared/neg1:tcp -h 127.0.0.2 -p 10000 -z").ice_twoway().ice_secure(false);
        const proxyB = communicator.stringToProxy("shared/neg2:tcp -h 127.0.0.2 -p 10000 -z").ice_twoway().ice_secure(false);
        const proxyC = communicator.stringToProxy("shared/neg3:tcp -h 127.0.0.2 -p 10000 -z").ice_twoway().ice_secure(false);
        
        const proxyD = communicator.stringToProxy("separate/neg1:tcp -h 127.0.0.2 -p 10000 -z").ice_twoway().ice_secure(false);
        const proxyE = communicator.stringToProxy("separate/neg2:tcp -h 127.0.0.2 -p 10000 -z").ice_twoway().ice_secure(false);
        const proxyF = communicator.stringToProxy("separate/neg3:tcp -h 127.0.0.2 -p 10000 -z").ice_twoway().ice_secure(false);

        let obj = await Demo.NegatorPrx.checkedCast(proxyA);

        menu();
        let line = null;
        do {
            process.stdout.write("==> ");
            for(line of await getline()) {
                try {
                    if (line == "x") {
                        break;
                    } else if (line == "a") {
                        obj = await Demo.NegatorPrx.checkedCast(proxyA);
                    } else if (line == "b") {
                        obj = await Demo.NegatorPrx.checkedCast(proxyB);
                    } else if (line == "c") {
                        obj = await Demo.NegatorPrx.checkedCast(proxyC);
                    } else if (line == "d") {
                        obj = await Demo.NegatorPrx.checkedCast(proxyD);
                    } else if (line == "e") {
                        obj = await Demo.NegatorPrx.checkedCast(proxyE);
                    } else if (line == "f") {
                        obj = await Demo.NegatorPrx.checkedCast(proxyF);
                    } else {
                        var number = parseInt(line);
                        if (isNaN(number)) {
                            console.log("Invalid command");
                        } else {
                            console.log(await obj.negate(number));
                        }
                    }
                    
                } catch(ex) {
                    console.log(ex.toString());
                }                
            }
        }
        while(line != "x");
    }
    catch(ex) {
        console.log(ex.toString());
        process.exitCode = 1;
    }
    finally
    {
        if(communicator)
        {
            await communicator.destroy();
        }
    }
})();

function menu()
{
    process.stdout.write(
`usage:
   a, b or c: set object to A, B or C. All of them have the same servant
   d, e or f: set object to D, E, or F. All of them have separate servants
   digit: send it to the server and get negation
   x: exit
`);
}

function getline()
{
    return new Promise(resolve => {
        process.stdin.resume();
        process.stdin.once("data", buffer => {
            process.stdin.pause();
            resolve(buffer.toString("utf-8").trim());
        });
    });
}
