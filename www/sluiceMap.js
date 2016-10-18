var exec = require('cordova/exec');

module.exports = {
    /**
     * show sluice list
     * @param userId
     * @param accessToken
     * @param serverUri
     */
    sluices: function (userId,accessToken,serverUri) {
        exec(null, null, "sluiceMapManager", "sluices", [userId,accessToken,serverUri]);
    },
    /**
     * show sluice detail
     * @param userId
     * @param accessToken
     * @param serverUri
     * @param id
     * @param Lgtd
     * @param LLtd
     * @param VideoCount
     */
    sluice: function (userId,accessToken,serverUri,id,Lgtd,LLtd,VideoCount) {
        exec(null, null, "sluiceMapManager", "sluice", [userId,accessToken,serverUri,id,Lgtd,LLtd,VideoCount]);
    }
};
