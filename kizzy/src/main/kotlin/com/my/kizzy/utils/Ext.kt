

package com.my.kizzy.utils

import com.my.kizzy.rpc.RpcImage

fun String.toRpcImage(): RpcImage? {
    return if (this.isBlank())
        null
    else if (this.startsWith("attachments"))
        RpcImage.DiscordImage(this)
    else
        RpcImage.ExternalImage(this)
}

