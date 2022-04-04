#include <jni.h>
#include <string>
#include <android/log.h>
#include <unistd.h>
#include <pthread.h>

// Inspired by https://codelab.wordpress.com/2014/11/03/how-to-use-standard-output-streams-for-logging-in-android-apps/

static int pfd[2];
static pthread_t thr;
static const char *tag = "stdio";

static void *android_logging_thread_func(void *ignored) {
    __android_log_write(ANDROID_LOG_DEBUG, tag,
                        "Starting to capture stdout/stderr to Android log.");
    ssize_t read_size;
    char buf[PAGE_SIZE];
    while ((read_size = read(pfd[0], buf, sizeof buf - 1)) > 0) {
        if (buf[read_size - 1] == '\n')
            --read_size;
        buf[read_size] = '\0'; /* add null-terminator */
        __android_log_write(ANDROID_LOG_DEBUG, tag, buf);
    }
    return 0;
}

int android_logging_start_logger() {
    /* make stdout line-buffered and stderr unbuffered */
    setvbuf(stdout, 0, _IOLBF, 0);
    setvbuf(stderr, 0, _IONBF, 0);

    /* create the pipe and redirect stdout and stderr */
    pipe(pfd);
    dup2(pfd[1], 1);
    dup2(pfd[1], 2);

    /* start the logging thread */
    if (pthread_create(&thr, 0, android_logging_thread_func, 0) == -1)
        return -1;
    pthread_detach(thr);
    printf("stdout now successfully routes into the stdio logger.\n");
    return 0;
}

extern "C" JNIEXPORT jboolean JNICALL
Java_org_beeware_android_MainActivity_captureStdoutStderr(
        JNIEnv *env,
        jobject /* this */) {
    return static_cast<jboolean>(android_logging_start_logger());
}
