// Import the standard library and add helper functions to the same namespace
require("wombat_lib.js")();

const handleEvent = (event, time_left) => {
    // Block some Node.js specific stuff
    const process = {
        env: 'These aren\'t the keys you\'re looking for.'
    };
    const require = null;
    
    try {
        const user_defined_code = eval(event.code);
        const response = user_defined_code(event.state, time_left);
        return {
            response,
            error: null
        };
    }
    catch(err) {
        return {
            response: null,
            error: {
                message: err.toString(),
                stackTrace: [err.stack]
            }
        };
    }
};

exports.handler = (event, context, callback) => {
    // This is used to know how much time you have left for your code
    const time_left = context.getRemainingTimeInMillis;
    callback(null, handleEvent(event, time_left));
};
