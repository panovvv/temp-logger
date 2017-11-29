package com.epam.vadim_panov;

import static com.fazecast.jSerialComm.SerialPort.TIMEOUT_WRITE_BLOCKING;

import com.fazecast.jSerialComm.SerialPort;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.Arrays;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.Timer;
import java.util.TimerTask;
import java.util.logging.Logger;
import java.util.stream.Collectors;
import javafx.animation.KeyFrame;
import javafx.animation.Timeline;
import javafx.application.Platform;
import javafx.collections.FXCollections;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.scene.Node;
import javafx.scene.control.Button;
import javafx.scene.control.CheckBox;
import javafx.scene.control.ComboBox;
import javafx.scene.control.Label;
import javafx.scene.control.ToggleButton;
import javafx.scene.layout.VBox;
import javafx.util.Duration;
import org.controlsfx.control.PopOver;

public class Controller {

	private static final Logger LOGGER = Logger.getLogger(Controller.class.getName());

	// Non-UI constants
	private static final int BAUD_RATE = 9600;
	private static final int DATA_BITS = 8;
	private static final int STOP_BITS = SerialPort.ONE_STOP_BIT;
	private static final int PARITY = SerialPort.NO_PARITY;
	private static final int SERIAL_READ_TIMEOUT_SEC = 5;
	private static final int SERIAL_WRITE_TIMEOUT = 5000;

	private static final String EMPTY_STRING = "";
	private static final String DATETIME_PATTERN = "yyyy-MM-dd hh:mm:ss a";
	private static final DateTimeFormatter DATETIME_FORMATTER = DateTimeFormatter.ofPattern(DATETIME_PATTERN);
	private static final String PLUS_SIGN = "+";
	private static final String MINUS_SIGN = "-";

	private static final String SET_TIME_PREFIX = "st";
	private static final String SET_TIME_POSTFIX = SET_TIME_PREFIX;
	private static final String SET_DELAY_PREFIX = "sd";
	private static final String SET_DELAY_POSTFIX = SET_DELAY_PREFIX;

	private static final String GET_TIME_COMMAND = "gt";
	private static final String GET_DELAY_COMMAND = "gd";
	private static final String TIME_TOKEN = "t";
	private static final String DELAY_TOKEN = "d";
	private static final String SUCCESS_MESSAGE = "Success!";
	private static final String FAILED_MESSAGE = "Failed!";

	@FXML
	private ComboBox<SerialPortWrapper> uartComboBox;
	@FXML
	private Button uartRefreshBtn;
	@FXML
	private Button uartConnectBtn;

	@FXML
	private Label datetimeSystemLabel;

	@FXML
	private CheckBox offsetCheckbox;
	@FXML
	private ToggleButton offsetSignBtn;
	@FXML
	private NumericTextField offsetHoursBox;
	@FXML
	private NumericTextField offsetMinutesBox;
	@FXML
	private NumericTextField offsetSecondsBox;
	@FXML
	private Button setTimeBtn;

	@FXML
	private NumericTextField delayDaysBox;
	@FXML
	private Label daysLabel;
	@FXML
	private NumericTextField delayHoursBox;
	@FXML
	private Label hoursLabel;
	@FXML
	private NumericTextField delayMinutesBox;
	@FXML
	private Label minutesLabel;
	@FXML
	private NumericTextField delaySecondBox;
	@FXML
	private Label secondsLabel;
	@FXML
	private Button setDelayBtn;

	@FXML
	private Label datetimeOnLoggerLabel;
	@FXML
	private Button getTimeBtn;

	@FXML
	private Label delayOnLoggerLabel;
	@FXML
	private Button getDelayBtn;

	// Non-UI variables
	private SerialPort serialPort;
	private Timer timeUpdater;

	private LocalDateTime currentDateTimeWithOffset;
	private boolean negativeOffset = false;

	@FXML
	void uartConnectBtnClicked(ActionEvent event) {
		if (!isSerialPortOpen()) {
			serialPort = uartComboBox.getSelectionModel().getSelectedItem().getPort();
			serialPort.setComPortParameters(BAUD_RATE, DATA_BITS, STOP_BITS, PARITY);
			serialPort.setComPortTimeouts(TIMEOUT_WRITE_BLOCKING,
				SERIAL_READ_TIMEOUT_SEC * 1000, SERIAL_WRITE_TIMEOUT);
			if (serialPort.openPort()) {
				setSerialPortConnectedOnUI(true);
			}
		} else {
			if (serialPort.closePort()) {
				setSerialPortConnectedOnUI(false);
				refreshSerialPortList();
			}
		}
	}

	private void setSerialPortConnectedOnUI(boolean isConnected) {
		uartConnectBtn.setText(isConnected ? "Disconnect" : "Connect");
		uartRefreshBtn.setDisable(isConnected);
		uartComboBox.setDisable(isConnected);
		offsetCheckbox.setDisable(!isConnected);
		setTimeBtn.setDisable(!isConnected);
		setDelayBtn.setDisable(!isConnected);
		getTimeBtn.setDisable(!isConnected);
		getDelayBtn.setDisable(!isConnected);
		delayDaysBox.setDisable(!isConnected);
		delayHoursBox.setDisable(!isConnected);
		delayMinutesBox.setDisable(!isConnected);
		delaySecondBox.setDisable(!isConnected);
		if (!isConnected) {
			delayDaysBox.setText(EMPTY_STRING);
			delayHoursBox.setText(EMPTY_STRING);
			delayMinutesBox.setText(EMPTY_STRING);
			delaySecondBox.setText(EMPTY_STRING);
		}
	}

	@FXML
	void uartRefreshBtnClicked(ActionEvent event) {
		refreshSerialPortList();
	}

	@FXML
	void offsetCheckboxChecked(ActionEvent event) {
		boolean addOffset = offsetCheckbox.isSelected();
		offsetSignBtn.setDisable(!addOffset);
		offsetHoursBox.setDisable(!addOffset);
		offsetMinutesBox.setDisable(!addOffset);
		offsetSecondsBox.setDisable(!addOffset);
		if (!addOffset) {
			offsetHoursBox.setText(EMPTY_STRING);
			offsetMinutesBox.setText(EMPTY_STRING);
			offsetSecondsBox.setText(EMPTY_STRING);
			negativeOffset = false;
			offsetSignBtn.setText(PLUS_SIGN);
		}
	}

	@FXML
	void offsetSignBtnToggled(ActionEvent event) {
		offsetSignBtn.setText(offsetSignBtn.isSelected() ? MINUS_SIGN : PLUS_SIGN);
		negativeOffset = offsetSignBtn.isSelected();
	}

	@FXML
	void setTimeBtnClicked(ActionEvent event) {
		if (isSerialPortOpen()) {
			LOGGER.info("Setting time to "
				+ currentDateTimeWithOffset.format(DATETIME_FORMATTER));
			long timestamp = currentDateTimeWithOffset.atZone(ZoneId.systemDefault()).toInstant().toEpochMilli() / 1000;
			LOGGER.info("Timestamp: " + timestamp);
			byte[] timestampBytes = String.format("%s%d%s", SET_TIME_PREFIX, timestamp, SET_TIME_POSTFIX).getBytes();
			if (serialPort.writeBytes(timestampBytes, timestampBytes.length) < 0) {
				showPopover(FAILED_MESSAGE, setTimeBtn);
			} else {
				showPopover(SUCCESS_MESSAGE, setTimeBtn);
				datetimeOnLoggerLabel.setText(EMPTY_STRING);
			}
		}
	}

	@FXML
	void setDelayBtnClicked(ActionEvent event) {
		if (isSerialPortOpen()) {
			LOGGER.info(String.format("Set delay to %d days %d hrs %d mins %d seconds.",
				delayDaysBox.getNumber(),
				delayHoursBox.getNumber(),
				delayMinutesBox.getNumber(),
				delaySecondBox.getNumber()));
			long delayInSeconds = delaySecondBox.getNumber()
				+ delayMinutesBox.getNumber() * 60
				+ delayHoursBox.getNumber() * 60 * 60
				+ delayDaysBox.getNumber() * 60 * 60 * 24;
			if (delayInSeconds == 0) {
				showPopover("Delay must a positive number!", setDelayBtn);
				return;
			}
			LOGGER.info("Delay in seconds: " + delayInSeconds);
			byte[] delayBytes = String.format("%s%d%s", SET_DELAY_PREFIX, delayInSeconds, SET_DELAY_POSTFIX).getBytes();
			if (serialPort.writeBytes(delayBytes, delayBytes.length) < 0) {
				showPopover(FAILED_MESSAGE, setDelayBtn);
			} else {
				showPopover(SUCCESS_MESSAGE, setDelayBtn);
				delayOnLoggerLabel.setText(EMPTY_STRING);
			}
		}
	}

	@FXML
	void getTimeBtnClicked(ActionEvent event) {
		if (isSerialPortOpen()) {
			Optional<Long> optionalTime = sendCommandAndReceiveResponse(GET_TIME_COMMAND, getTimeBtn, TIME_TOKEN);
			if (!optionalTime.isPresent()) {
				return;
			}

			long epochTime = optionalTime.get();
			LOGGER.info("Got time from device: " + epochTime);
			datetimeOnLoggerLabel.setText(LocalDateTime.ofInstant(Instant.ofEpochSecond(epochTime), ZoneId
				.systemDefault()).format(DATETIME_FORMATTER));
			showPopover(SUCCESS_MESSAGE, getTimeBtn);
		}
	}


	@FXML
	void getDelayBtnClicked(ActionEvent event) {
		if (isSerialPortOpen()) {
			Optional<Long> optionalDelay = sendCommandAndReceiveResponse(GET_DELAY_COMMAND, getDelayBtn, DELAY_TOKEN);
			if (!optionalDelay.isPresent()) {
				return;
			}

			long delay = optionalDelay.get();
			LOGGER.info("Got delay from device: " + delay);
			int days = (int) (delay / (60 * 60 * 24));
			delay -= days * (60 * 60 * 24);
			int hours = (int) (delay / (60 * 60));
			delay -= hours * (60 * 60);
			int minutes = (int) (delay / 60);
			delay -= minutes * 60;

			StringBuilder delayBuilder = new StringBuilder();
			if (days > 0) {
				delayBuilder.append(String.format("%d day", days));
				if (isPluralNumber(days)) {
					delayBuilder.append("s");
				}
			}
			if (hours > 0) {
				delayBuilder.append(String.format(" %d hour", hours));
				if (isPluralNumber(hours)) {
					delayBuilder.append("s");
				}
			}
			if (minutes > 0) {
				delayBuilder.append(String.format(" %d minute", minutes));
				if (isPluralNumber(minutes)) {
					delayBuilder.append("s");
				}
			}
			if (delay > 0) {
				delayBuilder.append(String.format(" %d second", delay));
				if (isPluralNumber((int) delay)) {
					delayBuilder.append("s");
				}
			}
			delayOnLoggerLabel.setText(delayBuilder.toString());
			showPopover(SUCCESS_MESSAGE, getDelayBtn);
		}
	}

	private Optional<Long> sendCommandAndReceiveResponse(String command, Node sender, String markingToken) {
		if (serialPort.writeBytes(command.getBytes(), command.getBytes().length) < 0) {
			showPopover("Can't send command!", sender);
			LOGGER.warning("Can't send command!");
			return Optional.empty();
		}
		LOGGER.info("Sent the command");

		StringBuilder result = new StringBuilder();
		char tokensReceived = 0;
		try {
			LocalDateTime timeout = LocalDateTime.now().plusSeconds(SERIAL_READ_TIMEOUT_SEC);
			clearUartBuffer();
			while ((tokensReceived < 2) && (LocalDateTime.now().compareTo(timeout) < 0)) {
				if (serialPort.bytesAvailable() == 0) {
					Thread.sleep(20);
				} else {
					byte[] readBuffer = new byte[serialPort.bytesAvailable()];
					serialPort.readBytes(readBuffer, readBuffer.length);
					String in = new String(readBuffer);
					if (in.contains(markingToken)) {
						tokensReceived++;
					}
					result.append(in);
				}
			}
		} catch (InterruptedException e) {
			LOGGER.warning("Can't perform delay");
			e.printStackTrace();
		}

		if (tokensReceived < 2) {
			LOGGER.warning("Received invalid string while trying to get data from device: " + result.toString());
			showPopover("Data not received due to timeout. Check your connections and cycle the power to logger",
				sender);
			return Optional.empty();
		}

		String numberStr = EMPTY_STRING;
		try {
			numberStr = result.substring(result.indexOf(markingToken) + 1, result.lastIndexOf(markingToken));
			return Optional.of(Long.parseLong(numberStr));
		} catch (NumberFormatException nfe) {
			LOGGER.warning("Received invalid number while trying to get time from device: " + numberStr);
			showPopover("Time received from device is not a number!", sender);
		}

		return Optional.empty();
	}

	private void clearUartBuffer() {
		byte[] buffer = new byte[1];
		while (serialPort.bytesAvailable() > 0) {
			serialPort.readBytes(buffer, 1);
		}
	}

	private boolean isPluralNumber(int n) {
		return !((n % 10 == 1) && (n != 11));
	}

	@FXML
	void uartComboBoxChanged(ActionEvent event) {

	}

	@FXML
	void initialize() {
		assert setTimeBtn != null : "fx:id=\"setTimeBtn\" was not injected: check your FXML file 'scene.fxml'.";
		assert offsetSignBtn != null : "fx:id=\"offsetSignBtn\" was not injected: check your FXML file 'scene.fxml'.";
		assert datetimeOnLoggerLabel
			!= null : "fx:id=\"datetimeOnLoggerLabel\" was not injected: check your FXML file 'scene.fxml'.";
		assert uartComboBox != null : "fx:id=\"uartComboBox\" was not injected: check your FXML file 'scene.fxml'.";
		assert offsetCheckbox != null : "fx:id=\"offsetCheckbox\" was not injected: check your FXML file 'scene.fxml'.";
		assert getTimeBtn != null : "fx:id=\"getTimeBtn\" was not injected: check your FXML file 'scene.fxml'.";
		assert daysLabel != null : "fx:id=\"daysLabel\" was not injected: check your FXML file 'scene.fxml'.";
		assert delayHoursBox != null : "fx:id=\"delayHoursBox\" was not injected: check your FXML file 'scene.fxml'.";
		assert minutesLabel != null : "fx:id=\"minutesLabel\" was not injected: check your FXML file 'scene.fxml'.";
		assert offsetMinutesBox
			!= null : "fx:id=\"offsetMinutesBox\" was not injected: check your FXML file 'scene.fxml'.";
		assert offsetHoursBox != null : "fx:id=\"offsetHoursBox\" was not injected: check your FXML file 'scene.fxml'.";
		assert setDelayBtn != null : "fx:id=\"setDelayBtn\" was not injected: check your FXML file 'scene.fxml'.";
		assert hoursLabel != null : "fx:id=\"hoursLabel\" was not injected: check your FXML file 'scene.fxml'.";
		assert delaySecondBox != null : "fx:id=\"delaySecondBox\" was not injected: check your FXML file 'scene.fxml'.";
		assert getDelayBtn != null : "fx:id=\"getDelayBtn\" was not injected: check your FXML file 'scene.fxml'.";
		assert
			delayMinutesBox != null : "fx:id=\"delayMinutesBox\" was not injected: check your FXML file 'scene.fxml'.";
		assert secondsLabel != null : "fx:id=\"secondsLabel\" was not injected: check your FXML file 'scene.fxml'.";
		assert delayDaysBox != null : "fx:id=\"delayDaysBox\" was not injected: check your FXML file 'scene.fxml'.";
		assert uartConnectBtn != null : "fx:id=\"uartConnectBtn\" was not injected: check your FXML file 'scene.fxml'.";
		assert datetimeSystemLabel
			!= null : "fx:id=\"datetimeSystemLabel\" was not injected: check your FXML file 'scene.fxml'.";
		assert offsetSecondsBox
			!= null : "fx:id=\"offsetSecondsBox\" was not injected: check your FXML file 'scene.fxml'.";
		assert delayOnLoggerLabel
			!= null : "fx:id=\"delayOnLoggerLabel\" was not injected: check your FXML file 'scene.fxml'.";
	}

	public void close() {
		if (isSerialPortOpen()) {
			serialPort.closePort();
		}
		timeUpdater.cancel();
	}

	public void start() {
		refreshSerialPortList();
		timeUpdater = new Timer();
		timeUpdater.scheduleAtFixedRate(new TimerTask() {
			@Override
			public void run() {
				Platform.runLater(() -> {
					currentDateTimeWithOffset = LocalDateTime.now()
						.plusHours(negativeOffset ? -offsetHoursBox.getNumber() : offsetHoursBox.getNumber())
						.plusMinutes(negativeOffset ? -offsetMinutesBox.getNumber() : offsetMinutesBox.getNumber())
						.plusSeconds(negativeOffset ? -offsetSecondsBox.getNumber() : offsetSecondsBox.getNumber());
					datetimeSystemLabel
						.setText(currentDateTimeWithOffset.format(DATETIME_FORMATTER));
				});
			}
		}, 0, 1000);
	}

	private void refreshSerialPortList() {
		List<SerialPortWrapper> ports = Arrays.stream(SerialPort.getCommPorts())
			.map(SerialPortWrapper::new)
			.collect(Collectors.toList());
		SerialPortWrapper selectedItem = uartComboBox.getSelectionModel().getSelectedItem();
		uartComboBox.setItems(FXCollections.observableArrayList(ports));
		if (ports.contains(selectedItem)) {
			uartComboBox.getSelectionModel().select(selectedItem);
		} else {
			uartComboBox.getSelectionModel().selectFirst();
		}
		selectedItem = uartComboBox.getSelectionModel().getSelectedItem();
		uartConnectBtn.setDisable(selectedItem == null);
	}

	private boolean isSerialPortOpen() {
		if (Objects.nonNull(serialPort) && serialPort.isOpen()) {
			return true;
		} else {
			return false;
		}
	}

	void showPopover(String message, Node owner) {
		PopOver popOver = new PopOver();
		popOver.setContentNode(new VBox(new Label(EMPTY_STRING),
			new Label(message),
			new Label(EMPTY_STRING)));
		popOver.setFadeInDuration(Duration.valueOf("1s"));
		popOver.setFadeOutDuration(Duration.valueOf("1s"));
		popOver.show(owner);
		Timeline idlestage = new Timeline(new KeyFrame(Duration.seconds(2), event1 -> popOver.hide()));
		idlestage.setCycleCount(1);
		idlestage.play();
	}

}
