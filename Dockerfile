FROM openjdk:17-jdk-slim

# Install required packages
RUN apt-get update && apt-get install -y \
    curl \
    unzip \
    git \
    && rm -rf /var/lib/apt/lists/*

# Set up environment variables for Android SDK
ENV ANDROID_HOME=/opt/android-sdk
ENV PATH=${PATH}:${ANDROID_HOME}/cmdline-tools/latest/bin:${ANDROID_HOME}/platform-tools

# Create directories
RUN mkdir -p ${ANDROID_HOME}

# Download and install Android command line tools
RUN cd ${ANDROID_HOME} && \
    curl -o cmdline-tools.zip https://dl.google.com/android/repository/commandlinetools-linux-10406996_latest.zip && \
    unzip cmdline-tools.zip && \
    mkdir -p cmdline-tools/latest && \
    mv cmdline-tools/* cmdline-tools/latest/ 2>/dev/null || true && \
    rm cmdline-tools.zip

# Accept licenses and install required Android SDK components
RUN yes | sdkmanager --licenses && \
    sdkmanager "platform-tools" "platforms;android-35" "build-tools;34.0.0"

# Set working directory
WORKDIR /app

# Copy the project
COPY . .

# Build the app
CMD ["./gradlew", "assembleDebug"] 