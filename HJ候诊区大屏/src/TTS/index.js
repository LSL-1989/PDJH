const synth = window.speechSynthesis

/**
 * 文字转语音
 * @author 袁首京
 */
export default {

    /**
       * 语音提醒
       * @param {String} text
       */
    speek(text) {
        const ssu = new SpeechSynthesisUtterance(text)
        synth.speak(ssu)
    },

    /**
     * 退出语音
     */
    cancel() {
        synth.cancel()
    }
}

