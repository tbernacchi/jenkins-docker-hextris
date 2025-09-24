def job = Jenkins.instance.getItem("teste")

job.getBuilds().each { build ->
    if (build.result == hudson.model.Result.FAILURE) {
        println("Deletando build #${build.number}")
        build.delete()
    }
}
