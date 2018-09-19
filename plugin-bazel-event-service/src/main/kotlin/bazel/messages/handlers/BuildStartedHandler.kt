package bazel.messages.handlers

import bazel.HandlerPriority
import bazel.Verbosity
import bazel.atLeast
import bazel.bazel.events.BazelEvent
import bazel.bazel.events.BuildStarted
import bazel.messages.Color
import bazel.messages.ServiceMessageContext
import bazel.messages.apply

class BuildStartedHandler: EventHandler {
    override val priority: HandlerPriority
        get() = HandlerPriority.Low

    override fun handle(ctx: ServiceMessageContext) =
        if (ctx.event.payload is BazelEvent && ctx.event.payload.content is BuildStarted) {
            val event = ctx.event.payload.content
            val description = event.command
            val details = ctx.buildMessage(false)
                    .append(description.apply(Color.BuildStage), Verbosity.Normal)
                    .append(" v${event.buildToolVersion}", Verbosity.Verbose)
                    .append(", directory: \"${event.workingDirectory}\"", Verbosity.Verbose)
                    .append(", workspace: \"${event.workspaceDirectory}\"", Verbosity.Verbose)
                    .toString()

            if(ctx.verbosity.atLeast(Verbosity.Normal)) {
                if (ctx.blockManager.createBlock(description, event.children)) {
                    ctx.onNext(ctx.messageFactory.createBlockOpened(description, ""))
                }
            }

            ctx.onNext(ctx.messageFactory.createBuildStatus(description))
            if(ctx.verbosity.atLeast(Verbosity.Detailed)) {
                ctx.onNext(ctx.messageFactory.createMessage(details))
            }

            true
        } else ctx.handlerIterator.next().handle(ctx)
}