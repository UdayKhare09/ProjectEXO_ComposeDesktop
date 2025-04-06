# ProjectEXO - Secure Chat Application

ProjectEXO is a secure messaging application built with Kotlin and Jetpack Compose for desktop. It provides end-to-end encrypted communications with RSA encryption, real-time messaging, file sharing, and AI assistant integration.

## Features

- **Secure Communication**: End-to-end encryption using RSA
- **Real-time Messaging**: Instant private and group messaging
- **File Sharing**: Securely send and receive images
- **AI Assistant**: Integrated AI chat capabilities
- **Modern UI**: Built with Jetpack Compose for desktop

## Technical Architecture

The application follows a client-server architecture:

- **Client**: Kotlin application with Jetpack Compose UI
- **Server**: Java backend (not included in this repository)
- **Encryption**: RSA encryption for all communications
- **Protocol**: Custom binary packet protocol for efficient communication

## Getting Started

### Prerequisites

- JDK 17 or higher
- Gradle 7.6 or higher

### Building the Application

```bash
./gradlew build
```

### Running the Application

```bash
./gradlew run
```

### Creating a Distribution Package

```bash
./gradlew packageDmg       # for macOS
./gradlew packageMsi       # for Windows
./gradlew packageDeb       # for Linux
```

## Project Structure

- **UI Components**:
    - `Login.kt`: User authentication screen
    - `Chat.kt`: Main chat interface
    - `FeatureScreen.kt`: Feature selection screen
    - `Navigation.kt`: Application navigation

- **Networking**:
    - `ClientSocket.kt`: Manages socket connection and encryption
    - `PacketHandler.kt`: Processes incoming packets
    - `MsgHandler.kt`: Handles message-related packets
    - `ImageHandler.kt`: Manages image transmission
    - `AIPackets.kt`: Handles AI-related communications

## Security

The application employs RSA encryption for all communications:
- Key exchange during initial connection
- Authentication using encrypted credentials
- All messages and files are encrypted before transmission

## License

This project is licensed under the MIT License - see the LICENSE file for details.

## Contributing

Contributions are welcome! Please feel free to submit a Pull Request.