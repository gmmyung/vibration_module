# Unit Testing Guide for Voice-Text Chat Simulator

## What is Unit Testing?

**Unit Testing** is a software testing method where individual units or components of a software application are tested in isolation. A "unit" is the smallest testable part of an application.

### In Our Context:
- **Unit** = Individual methods, functions, or classes
- **Isolation** = Testing without external dependencies
- **Focus** = Testing business logic, state management, and data handling

## How to Define a "Unit" in Our App

### 1. **Method-Level Units**
Each public method in `ChatViewModel` is a unit:
```kotlin
// These are individual units:
fun hasRecordAudioPermission(context: Context): Boolean
fun onPermissionGranted()
fun addVoiceMessage(content: String)
fun addTextMessage(content: String)
```

### 2. **State Management Units**
Each StateFlow and its behavior is a unit:
```kotlin
// These are units:
val messages: StateFlow<List<ChatMessage>>
val isListening: StateFlow<Boolean>
val sttStatus: StateFlow<SttStatus>
```

### 3. **Data Class Units**
Each property and behavior of data classes:
```kotlin
// These are units:
data class ChatMessage(...)
sealed class SttStatus
```

### 4. **Business Logic Units**
Specific business rules and logic:
```kotlin
// These are units:
private fun cleanRecognizedText(text: String): String
private fun getSttErrorMessage(errorCode: Int): String
```

## Unit Testing Structure

### AAA Pattern (Arrange-Act-Assert)

```kotlin
@Test
fun `test method should behave correctly`() {
    // ARRANGE: Setup test data and mocks
    val testData = "test input"
    every { mockObject.method() } returns "expected result"
    
    // ACT: Execute the method under test
    val result = systemUnderTest.method(testData)
    
    // ASSERT: Verify the expected outcome
    assertEquals("expected result", result)
    verify { mockObject.method() }
}
```

## Testing Dependencies

### Mocking Strategy
We use **MockK** for mocking external dependencies:

```kotlin
// Mock external dependencies
private lateinit var mockContext: Context
private lateinit var mockSpeechRecognizer: SpeechRecognizer

// Setup mocks
mockContext = mockk(relaxed = true)
mockSpeechRecognizer = mockk(relaxed = true)

// Mock static methods
mockkStatic(SpeechRecognizer::class)
every { SpeechRecognizer.isRecognitionAvailable(any()) } returns true
```

### Coroutines Testing
We use **kotlinx-coroutines-test** for testing coroutines:

```kotlin
@OptIn(ExperimentalCoroutinesApi::class)
class ChatViewModelTest {
    private val testDispatcher = StandardTestDispatcher()
    
    @Before
    fun setup() {
        Dispatchers.setMain(testDispatcher)
    }
    
    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }
}
```

## What We Test

### ✅ **What We DO Test:**
1. **Business Logic**: Message processing, state management
2. **Data Validation**: Input validation, edge cases
3. **State Changes**: StateFlow updates, state transitions
4. **Error Handling**: Error scenarios, exception handling
5. **Data Classes**: Properties, equality, computed properties
6. **Sealed Classes**: Type checking, pattern matching

### ❌ **What We DON'T Test:**
1. **Android Framework**: Context behavior, system services
2. **UI Interactions**: Compose UI, user interactions
3. **Network Calls**: External API calls
4. **File I/O**: File system operations
5. **Database Operations**: Data persistence

## Test Categories

### 1. **ChatViewModel Tests**
- Permission checking
- Message management
- State management
- Error handling
- STT status management

### 2. **ChatMessage Tests**
- Data class construction
- Property validation
- Equality behavior
- Computed properties (formattedTime)

### 3. **SttStatus Tests**
- Sealed class behavior
- Object instances
- Data class instances
- Type checking
- Pattern matching

## Running Tests

### Run All Tests
```bash
./gradlew test
```

### Run Specific Test Class
```bash
./gradlew test --tests "ChatViewModelTest"
```

### Run Specific Test Method
```bash
./gradlew test --tests "ChatViewModelTest.hasRecordAudioPermission should return true when permission is granted"
```

## Test Naming Convention

### Method Names
Use descriptive names that explain the expected behavior:
```kotlin
@Test
fun `hasRecordAudioPermission should return true when permission is granted`()

@Test
fun `addVoiceMessage should add message with VOICE_USER sender type`()

@Test
fun `formattedTime should return time in HH:mm format`()
```

### Test Structure
- **Given/When/Then** or **Arrange/Act/Assert**
- Clear setup, execution, and verification
- One assertion per test (when possible)
- Descriptive error messages

## Benefits of Unit Testing

### 1. **Early Bug Detection**
- Catch bugs before they reach production
- Identify issues during development

### 2. **Documentation**
- Tests serve as living documentation
- Show expected behavior of methods

### 3. **Refactoring Safety**
- Confidence to refactor code
- Ensure behavior doesn't change

### 4. **Design Improvement**
- Force better code design
- Identify tight coupling

### 5. **Regression Prevention**
- Prevent old bugs from returning
- Maintain code quality over time

## Best Practices

### 1. **Test Isolation**
- Each test should be independent
- No shared state between tests
- Use `@Before` and `@After` for setup/cleanup

### 2. **Mock External Dependencies**
- Mock Android framework components
- Mock network calls
- Mock file system operations

### 3. **Test Edge Cases**
- Empty inputs
- Null values
- Boundary conditions
- Error scenarios

### 4. **Keep Tests Simple**
- One concept per test
- Clear and readable
- Fast execution

### 5. **Use Descriptive Names**
- Explain what is being tested
- Include expected behavior
- Make failures easy to understand

## Example Test Execution

```bash
# Run all unit tests
./gradlew test

# Expected output:
# ChatViewModelTest > hasRecordAudioPermission should return true when permission is granted PASSED
# ChatViewModelTest > addVoiceMessage should add message with VOICE_USER sender type PASSED
# ChatMessageTest > formattedTime should return time in HH:mm format PASSED
# SttStatusTest > SttStatus object instances should be singletons PASSED
# 
# BUILD SUCCESSFUL in 2s
# 15 actionable tasks: 15 executed
```

## Troubleshooting

### Common Issues

1. **MockK Static Mocking**
   ```kotlin
   // Make sure to use mockkStatic
   mockkStatic(SpeechRecognizer::class)
   ```

2. **Coroutines Testing**
   ```kotlin
   // Use runTest for coroutine tests
   @Test
   fun testCoroutine() = runTest {
       // Test coroutine code here
   }
   ```

3. **StateFlow Testing**
   ```kotlin
   // Collect StateFlow values in tests
   val collectedValues = mutableListOf<Type>()
   val job = launch {
       viewModel.stateFlow.collect { collectedValues.add(it) }
   }
   ```

This guide provides a comprehensive understanding of unit testing in our Voice-Text Chat Simulator, focusing on how to define units and structure effective tests.
