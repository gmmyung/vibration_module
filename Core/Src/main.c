/* USER CODE BEGIN Header */
/**
 ******************************************************************************
 * @file           : main.c
 * @brief          : Main program body
 ******************************************************************************
 * @attention
 *
 * Copyright (c) 2025 STMicroelectronics.
 * All rights reserved.
 *
 * This software is licensed under terms that can be found in the LICENSE file
 * in the root directory of this software component.
 * If no LICENSE file comes with this software, it is provided AS-IS.
 *
 ******************************************************************************
 */
/* USER CODE END Header */
/* Includes ------------------------------------------------------------------*/
#include "main.h"

/* Private includes ----------------------------------------------------------*/
/* USER CODE BEGIN Includes */
#include <stdbool.h>
#include <stdio.h>
#include <string.h>
/* USER CODE END Includes */

/* Private typedef -----------------------------------------------------------*/
/* USER CODE BEGIN PTD */

/* USER CODE END PTD */

/* Private define ------------------------------------------------------------*/
/* USER CODE BEGIN PD */
/* USER CODE END PD */

/* Private macro -------------------------------------------------------------*/
/* USER CODE BEGIN PM */

/* USER CODE END PM */

/* Private variables ---------------------------------------------------------*/
I2C_HandleTypeDef hi2c1;
I2C_HandleTypeDef hi2c2;

TIM_HandleTypeDef htim2;

UART_HandleTypeDef huart2;

/* USER CODE BEGIN PV */
static volatile bool g_touch_event = false;
/* USER CODE END PV */

/* Private function prototypes -----------------------------------------------*/
void SystemClock_Config(void);
static void MX_GPIO_Init(void);
static void MX_I2C1_Init(void);
static void MX_I2C2_Init(void);
static void MX_TIM2_Init(void);
static void MX_USART2_UART_Init(void);
/* USER CODE BEGIN PFP */
static void handle_tch_out_interrupt(void);
/* USER CODE END PFP */

/* Private user code ---------------------------------------------------------*/
/* USER CODE BEGIN 0 */

// DRV2605L I2C address: 7-bit 0x5A (HAL expects left-shifted)
#define DRV2605_ADDR (0x5A << 1)

/* Key registers */
#define DRV2605_REG_STATUS 0x00
#define DRV2605_REG_MODE 0x01
#define DRV2605_REG_RTP_INPUT 0x02
#define DRV2605_REG_LIBRARY 0x03
#define DRV2605_REG_WAVESEQ1 0x04 // 0x04..0x0B
#define DRV2605_REG_GO 0x0C
#define DRV2605_REG_RATEDV 0x16
#define DRV2605_REG_CLAMPV 0x17
#define DRV2605_REG_FEEDBACK 0x1A
#define DRV2605_REG_CTRL1 0x1B
#define DRV2605_REG_CTRL2 0x1C
#define DRV2605_REG_CTRL3 0x1D
#define DRV2605_REG_AUTOCAL 0x1E

/* Library select: bits [2:0]; 6 = LRA library per datasheet */
#define DRV2605_LIB_LRA 0x06

/* MODE register [2:0] */
#define DRV2605_MODE_INTTRIG 0x00
#define DRV2605_MODE_AUTOCAL 0x07

/* Example effect IDs from ROM: 1 = Strong Click (100%) */
#define DRV2605_EFFECT_STRONG_CLICK_100 1

/* Typical LRA defaults from TI’s setup guide:
   RATED_VOLT_CODE for ~2 Vrms  => 0x53
   OD_CLAMP_CODE  for ~3 Vpeak  => 0x89
   Feedback/Control registers for LRA closed-loop library playback:
   FEEDBACK 0x1A = 0xB6 (bit7 LRA select + recommended gains)
   CTRL1    0x1B = 0x13
   CTRL2    0x1C = 0xF5
   CTRL3    0x1D = 0x80
   You can tweak RATED/CLAMP codes for your actuator; see formulas below. */
#define DRV2605_RATED_VOLT_CODE 0x53
#define DRV2605_OD_CLAMP_CODE 0x89

/* Helper: I2C single-byte write/read */
static HAL_StatusTypeDef drv2605_write(uint8_t reg, uint8_t val) {
  return HAL_I2C_Mem_Write(&hi2c1, DRV2605_ADDR, reg, I2C_MEMADD_SIZE_8BIT,
                           &val, 1, 100);
}
static HAL_StatusTypeDef drv2605_read(uint8_t reg, uint8_t *val) {
  return HAL_I2C_Mem_Read(&hi2c1, DRV2605_ADDR, reg, I2C_MEMADD_SIZE_8BIT, val,
                          1, 100);
}

/* Wait for GO bit to clear after autocal / playback */
static HAL_StatusTypeDef drv2605_wait_go_clear(uint32_t timeout_ms) {
  uint32_t start = HAL_GetTick();
  uint8_t go = 1;
  while ((HAL_GetTick() - start) < timeout_ms) {
    if (drv2605_read(DRV2605_REG_GO, &go) != HAL_OK)
      return HAL_ERROR;
    if ((go & 0x01) == 0)
      return HAL_OK;
  }
  return HAL_TIMEOUT;
}

/* Initialize DRV2605L for LRA, closed-loop, ROM library playback + autocal */
static void drv2605_init_lra(void) {
  HAL_Delay(1); // TI: wait >=250us after power-up for I2C
  HAL_GPIO_WritePin(VIB_EN_GPIO_Port, VIB_EN_Pin, GPIO_PIN_SET); // enable DRV
  HAL_Delay(1);

  /* Exit standby, default to internal trigger mode */
  if (drv2605_write(DRV2605_REG_MODE, DRV2605_MODE_INTTRIG) != HAL_OK)
    Error_Handler();

  /* Select LRA library */
  if (drv2605_write(DRV2605_REG_LIBRARY, DRV2605_LIB_LRA) != HAL_OK)
    Error_Handler();

  /* Program typical LRA settings (can keep defaults, these are good starting
   * points) */
  if (drv2605_write(DRV2605_REG_RATEDV, DRV2605_RATED_VOLT_CODE) != HAL_OK)
    Error_Handler();
  if (drv2605_write(DRV2605_REG_CLAMPV, DRV2605_OD_CLAMP_CODE) != HAL_OK)
    Error_Handler();

  if (drv2605_write(DRV2605_REG_FEEDBACK, 0xB6) != HAL_OK) // LRA + gains
    Error_Handler();
  if (drv2605_write(DRV2605_REG_CTRL1, 0x13) != HAL_OK)
    Error_Handler();
  if (drv2605_write(DRV2605_REG_CTRL2, 0xF5) != HAL_OK)
    Error_Handler();
  if (drv2605_write(DRV2605_REG_CTRL3, 0x80) != HAL_OK)
    Error_Handler();

  /* Optional: extend autocal time a bit (0x1E = 0x20 ~ 500 ms) */
  (void)drv2605_write(DRV2605_REG_AUTOCAL, 0x20);

  /* Run auto-calibration (MODE=0x07 then GO=1) */
  if (drv2605_write(DRV2605_REG_MODE, DRV2605_MODE_AUTOCAL) != HAL_OK)
    Error_Handler();
  if (drv2605_write(DRV2605_REG_GO, 0x01) != HAL_OK)
    Error_Handler();
  if (drv2605_wait_go_clear(2000) != HAL_OK) // up to ~2s
    Error_Handler();

  /* Back to internal trigger mode for library playback */
  if (drv2605_write(DRV2605_REG_MODE, DRV2605_MODE_INTTRIG) != HAL_OK)
    Error_Handler();

  /* Preload a one-shot waveform in slot 1 (slot2 = 0 terminates the sequence)
   */
  if (drv2605_write(DRV2605_REG_WAVESEQ1, DRV2605_EFFECT_STRONG_CLICK_100) !=
      HAL_OK)
    Error_Handler();
  if (drv2605_write(DRV2605_REG_WAVESEQ1 + 1, 0x00) != HAL_OK)
    Error_Handler();

  printf("DRV2605L initialized (LRA, library %d)\r\n", DRV2605_LIB_LRA);
}

/* Trigger playback of the preloaded effect */
static void drv2605_play(void) {
  /* If you power-save by deasserting EN, assert it here first */
  // HAL_GPIO_WritePin(VIB_EN_GPIO_Port, VIB_EN_Pin, GPIO_PIN_SET);

  /* Kick GO bit; it auto-clears when the effect finishes */
  if (drv2605_write(DRV2605_REG_GO, 0x01) != HAL_OK) {
    Error_Handler();
  }
}

/* USER CODE END 0 */

/**
 * @brief  The application entry point.
 * @retval int
 */
int main(void) {

  /* USER CODE BEGIN 1 */

  /* USER CODE END 1 */

  /* MCU Configuration--------------------------------------------------------*/

  /* Reset of all peripherals, Initializes the Flash interface and the Systick.
   */
  HAL_Init();

  /* USER CODE BEGIN Init */

  /* USER CODE END Init */

  /* Configure the system clock */
  SystemClock_Config();

  /* USER CODE BEGIN SysInit */

  /* USER CODE END SysInit */

  /* Initialize all configured peripherals */
  MX_GPIO_Init();
  MX_I2C1_Init();
  MX_I2C2_Init();
  MX_TIM2_Init();
  MX_USART2_UART_Init();
  /* USER CODE BEGIN 2 */
  printf("Starting...\r\n");
  HAL_TIM_Base_Start(&htim2);
  drv2605_init_lra();
  /* USER CODE END 2 */

  /* Infinite loop */
  /* USER CODE BEGIN WHILE */
  while (1) {
    /* USER CODE END WHILE */
    if (g_touch_event) {
      g_touch_event = false;
      drv2605_play();
      printf("Haptic: effect %d\r\n", DRV2605_EFFECT_STRONG_CLICK_100);
    }
    /* USER CODE BEGIN 3 */
  }
  /* USER CODE END 3 */
}

/**
 * @brief System Clock Configuration
 * @retval None
 */
void SystemClock_Config(void) {
  RCC_OscInitTypeDef RCC_OscInitStruct = {0};
  RCC_ClkInitTypeDef RCC_ClkInitStruct = {0};

  /** Configure the main internal regulator output voltage
   */
  HAL_PWREx_ControlVoltageScaling(PWR_REGULATOR_VOLTAGE_SCALE1);

  /** Initializes the RCC Oscillators according to the specified parameters
   * in the RCC_OscInitTypeDef structure.
   */
  RCC_OscInitStruct.OscillatorType = RCC_OSCILLATORTYPE_HSI;
  RCC_OscInitStruct.HSIState = RCC_HSI_ON;
  RCC_OscInitStruct.HSIDiv = RCC_HSI_DIV1;
  RCC_OscInitStruct.HSICalibrationValue = RCC_HSICALIBRATION_DEFAULT;
  RCC_OscInitStruct.PLL.PLLState = RCC_PLL_NONE;
  if (HAL_RCC_OscConfig(&RCC_OscInitStruct) != HAL_OK) {
    Error_Handler();
  }

  /** Initializes the CPU, AHB and APB buses clocks
   */
  RCC_ClkInitStruct.ClockType =
      RCC_CLOCKTYPE_HCLK | RCC_CLOCKTYPE_SYSCLK | RCC_CLOCKTYPE_PCLK1;
  RCC_ClkInitStruct.SYSCLKSource = RCC_SYSCLKSOURCE_HSI;
  RCC_ClkInitStruct.AHBCLKDivider = RCC_SYSCLK_DIV1;
  RCC_ClkInitStruct.APB1CLKDivider = RCC_HCLK_DIV1;

  if (HAL_RCC_ClockConfig(&RCC_ClkInitStruct, FLASH_LATENCY_0) != HAL_OK) {
    Error_Handler();
  }
}

/**
 * @brief I2C1 Initialization Function
 * @param None
 * @retval None
 */
static void MX_I2C1_Init(void) {

  /* USER CODE BEGIN I2C1_Init 0 */

  /* USER CODE END I2C1_Init 0 */

  /* USER CODE BEGIN I2C1_Init 1 */

  /* USER CODE END I2C1_Init 1 */
  hi2c1.Instance = I2C1;
  hi2c1.Init.Timing = 0x00503D58;
  hi2c1.Init.OwnAddress1 = 0;
  hi2c1.Init.AddressingMode = I2C_ADDRESSINGMODE_7BIT;
  hi2c1.Init.DualAddressMode = I2C_DUALADDRESS_DISABLE;
  hi2c1.Init.OwnAddress2 = 0;
  hi2c1.Init.OwnAddress2Masks = I2C_OA2_NOMASK;
  hi2c1.Init.GeneralCallMode = I2C_GENERALCALL_DISABLE;
  hi2c1.Init.NoStretchMode = I2C_NOSTRETCH_DISABLE;
  if (HAL_I2C_Init(&hi2c1) != HAL_OK) {
    Error_Handler();
  }

  /** Configure Analogue filter
   */
  if (HAL_I2CEx_ConfigAnalogFilter(&hi2c1, I2C_ANALOGFILTER_ENABLE) != HAL_OK) {
    Error_Handler();
  }

  /** Configure Digital filter
   */
  if (HAL_I2CEx_ConfigDigitalFilter(&hi2c1, 0) != HAL_OK) {
    Error_Handler();
  }
  /* USER CODE BEGIN I2C1_Init 2 */

  /* USER CODE END I2C1_Init 2 */
}

/**
 * @brief I2C2 Initialization Function
 * @param None
 * @retval None
 */
static void MX_I2C2_Init(void) {

  /* USER CODE BEGIN I2C2_Init 0 */

  /* USER CODE END I2C2_Init 0 */

  /* USER CODE BEGIN I2C2_Init 1 */

  /* USER CODE END I2C2_Init 1 */
  hi2c2.Instance = I2C2;
  hi2c2.Init.Timing = 0x00503D58;
  hi2c2.Init.OwnAddress1 = 0;
  hi2c2.Init.AddressingMode = I2C_ADDRESSINGMODE_7BIT;
  hi2c2.Init.DualAddressMode = I2C_DUALADDRESS_DISABLE;
  hi2c2.Init.OwnAddress2 = 0;
  hi2c2.Init.OwnAddress2Masks = I2C_OA2_NOMASK;
  hi2c2.Init.GeneralCallMode = I2C_GENERALCALL_DISABLE;
  hi2c2.Init.NoStretchMode = I2C_NOSTRETCH_DISABLE;
  if (HAL_I2C_Init(&hi2c2) != HAL_OK) {
    Error_Handler();
  }

  /** Configure Analogue filter
   */
  if (HAL_I2CEx_ConfigAnalogFilter(&hi2c2, I2C_ANALOGFILTER_ENABLE) != HAL_OK) {
    Error_Handler();
  }

  /** Configure Digital filter
   */
  if (HAL_I2CEx_ConfigDigitalFilter(&hi2c2, 0) != HAL_OK) {
    Error_Handler();
  }
  /* USER CODE BEGIN I2C2_Init 2 */

  /* USER CODE END I2C2_Init 2 */
}

/**
 * @brief TIM2 Initialization Function
 * @param None
 * @retval None
 */
static void MX_TIM2_Init(void) {

  /* USER CODE BEGIN TIM2_Init 0 */

  /* USER CODE END TIM2_Init 0 */

  TIM_MasterConfigTypeDef sMasterConfig = {0};
  TIM_OC_InitTypeDef sConfigOC = {0};

  /* USER CODE BEGIN TIM2_Init 1 */

  /* USER CODE END TIM2_Init 1 */
  htim2.Instance = TIM2;
  htim2.Init.Prescaler = 0;
  htim2.Init.CounterMode = TIM_COUNTERMODE_UP;
  htim2.Init.Period = 4294967295;
  htim2.Init.ClockDivision = TIM_CLOCKDIVISION_DIV1;
  htim2.Init.AutoReloadPreload = TIM_AUTORELOAD_PRELOAD_DISABLE;
  if (HAL_TIM_PWM_Init(&htim2) != HAL_OK) {
    Error_Handler();
  }
  sMasterConfig.MasterOutputTrigger = TIM_TRGO_RESET;
  sMasterConfig.MasterSlaveMode = TIM_MASTERSLAVEMODE_DISABLE;
  if (HAL_TIMEx_MasterConfigSynchronization(&htim2, &sMasterConfig) != HAL_OK) {
    Error_Handler();
  }
  sConfigOC.OCMode = TIM_OCMODE_PWM1;
  sConfigOC.Pulse = 0;
  sConfigOC.OCPolarity = TIM_OCPOLARITY_HIGH;
  sConfigOC.OCFastMode = TIM_OCFAST_DISABLE;
  if (HAL_TIM_PWM_ConfigChannel(&htim2, &sConfigOC, TIM_CHANNEL_1) != HAL_OK) {
    Error_Handler();
  }
  /* USER CODE BEGIN TIM2_Init 2 */

  /* USER CODE END TIM2_Init 2 */
  HAL_TIM_MspPostInit(&htim2);
}

/**
 * @brief USART2 Initialization Function
 * @param None
 * @retval None
 */
static void MX_USART2_UART_Init(void) {

  /* USER CODE BEGIN USART2_Init 0 */

  /* USER CODE END USART2_Init 0 */

  /* USER CODE BEGIN USART2_Init 1 */

  /* USER CODE END USART2_Init 1 */
  huart2.Instance = USART2;
  huart2.Init.BaudRate = 115200;
  huart2.Init.WordLength = UART_WORDLENGTH_8B;
  huart2.Init.StopBits = UART_STOPBITS_1;
  huart2.Init.Parity = UART_PARITY_NONE;
  huart2.Init.Mode = UART_MODE_TX_RX;
  huart2.Init.HwFlowCtl = UART_HWCONTROL_NONE;
  huart2.Init.OverSampling = UART_OVERSAMPLING_16;
  huart2.Init.OneBitSampling = UART_ONE_BIT_SAMPLE_DISABLE;
  huart2.Init.ClockPrescaler = UART_PRESCALER_DIV1;
  huart2.AdvancedInit.AdvFeatureInit = UART_ADVFEATURE_NO_INIT;
  if (HAL_UART_Init(&huart2) != HAL_OK) {
    Error_Handler();
  }
  /* USER CODE BEGIN USART2_Init 2 */

  /* USER CODE END USART2_Init 2 */
}

/**
 * @brief GPIO Initialization Function
 * @param None
 * @retval None
 */
static void MX_GPIO_Init(void) {
  GPIO_InitTypeDef GPIO_InitStruct = {0};
  /* USER CODE BEGIN MX_GPIO_Init_1 */

  /* USER CODE END MX_GPIO_Init_1 */

  /* GPIO Ports Clock Enable */
  __HAL_RCC_GPIOA_CLK_ENABLE();
  __HAL_RCC_GPIOB_CLK_ENABLE();

  /*Configure GPIO pin Output Level */
  HAL_GPIO_WritePin(GPIOB, VIB_EN_Pin | VIB_TRIG_Pin, GPIO_PIN_RESET);

  /*Configure GPIO pin : TCH_OUT_Pin */
  GPIO_InitStruct.Pin = TCH_OUT_Pin;
  GPIO_InitStruct.Mode = GPIO_MODE_IT_RISING_FALLING;
  GPIO_InitStruct.Pull = GPIO_NOPULL;
  HAL_GPIO_Init(TCH_OUT_GPIO_Port, &GPIO_InitStruct);

  /*Configure GPIO pins : VIB_EN_Pin VIB_TRIG_Pin */
  GPIO_InitStruct.Pin = VIB_EN_Pin | VIB_TRIG_Pin;
  GPIO_InitStruct.Mode = GPIO_MODE_OUTPUT_PP;
  GPIO_InitStruct.Pull = GPIO_NOPULL;
  GPIO_InitStruct.Speed = GPIO_SPEED_FREQ_LOW;
  HAL_GPIO_Init(GPIOB, &GPIO_InitStruct);

  /* EXTI interrupt init*/
  HAL_NVIC_SetPriority(EXTI0_1_IRQn, 0, 0);
  HAL_NVIC_EnableIRQ(EXTI0_1_IRQn);

  /* USER CODE BEGIN MX_GPIO_Init_2 */

  /* USER CODE END MX_GPIO_Init_2 */
}

/* USER CODE BEGIN 4 */
void HAL_GPIO_EXTI_Rising_Callback(uint16_t GPIO_Pin) {
  if (GPIO_Pin == TCH_OUT_Pin) {
    g_touch_event = true;
  }
}

void HAL_GPIO_EXTI_Falling_Callback(uint16_t GPIO_Pin) {
  if (GPIO_Pin == TCH_OUT_Pin) {
    g_touch_event = true;
  }
}
/* USER CODE END 4 */

/**
 * @brief  This function is executed in case of error occurrence.
 * @retval None
 */
void Error_Handler(void) {
  /* USER CODE BEGIN Error_Handler_Debug */
  /* User can add his own implementation to report the HAL error return state */
  __disable_irq();
  while (1) {
  }
  /* USER CODE END Error_Handler_Debug */
}
#ifdef USE_FULL_ASSERT
/**
 * @brief  Reports the name of the source file and the source line number
 *         where the assert_param error has occurred.
 * @param  file: pointer to the source file name
 * @param  line: assert_param error line source number
 * @retval None
 */
void assert_failed(uint8_t *file, uint32_t line) {
  /* USER CODE BEGIN 6 */
  /* User can add his own implementation to report the file name and line
     number, ex: printf("Wrong parameters value: file %s on line %d\r\n", file,
     line) */
  /* USER CODE END 6 */
}
#endif /* USE_FULL_ASSERT */
