import javax.swing.*;
import javax.swing.border.*;
import java.awt.*;
import java.awt.event.*;
import java.net.*;
import java.io.*;
import java.text.SimpleDateFormat;
import java.util.*;
import java.util.List;
import org.json.*;

/**
 * Enhanced Modern Weather GUI with favorites, unit toggle, and better UI.
 * Features:
 * - Search any city worldwide (Dynamic search already implemented)
 * - Save favorite cities
 * - Toggle between Celsius and Fahrenheit
 * - Auto-refresh capability
 * - Real weather icons and descriptions from API
 * - 5-day forecast preview
 * - Feels-like temperature
 * - Pressure and visibility data
 * * NOTE: You MUST replace "YOUR_API_KEY" with your OpenWeatherMap API key on line 663.
 * Requires org.json library (json-20231013.jar) on classpath.
 */
public class WeatherSystem_API_Java extends JFrame {
    // UI components
    private final JTextField searchField = new JTextField();
    private final JButton searchButton = new JButton("Search");
    private final JLabel cityLabel = new JLabel("--", SwingConstants.CENTER);
    private final JLabel mainIcon = new JLabel("", SwingConstants.CENTER);
    private final JLabel tempLabel = new JLabel("--°", SwingConstants.CENTER);
    private final JLabel feelsLikeLabel = new JLabel("Feels like: --°", SwingConstants.CENTER);
    private final JLabel descLabel = new JLabel("--", SwingConstants.CENTER);
    private final JLabel humidityLabel = new JLabel("-- %");
    private final JLabel windLabel = new JLabel("-- m/s");
    private final JLabel pressureLabel = new JLabel("-- hPa");
    private final JLabel visibilityLabel = new JLabel("-- km");
    private final JLabel sunriseLabel = new JLabel("--");
    private final JLabel sunsetLabel = new JLabel("--");
    private final JLabel updatedLabel = new JLabel("Last updated: --");
    private final JButton autoButton = new JButton("Auto Refresh");
    private final JButton stopAutoButton = new JButton("Stop");
    private final JButton addFavoriteButton = new JButton("★ Add Favorite");
    private final JComboBox<String> favoritesCombo = new JComboBox<>();
    private final JToggleButton unitToggle = new JToggleButton("°F");
    private final JPanel forecastPanel = new JPanel(new GridLayout(1, 5, 8, 8));
    
    private final javax.swing.Timer autoTimer;
    private String currentCity = "Noida";
    private boolean isCelsius = true;
    private final Set<String> favoriteCities = new LinkedHashSet<>();
    private WeatherData currentWeatherData;

    private final WeatherStation station = new WeatherStation();

    public WeatherSystem_API_Java() {
        super("Weather Monitoring System • Live Forecast");
        setSize(900, 700);
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setLocationRelativeTo(null);
        setLayout(new BorderLayout(12, 12));
        getContentPane().setBackground(new Color(235, 245, 250));

        // Initialize favorites
        favoriteCities.add("London");
        favoriteCities.add("New York");
        favoriteCities.add("Tokyo");
        favoriteCities.add("Paris");
        favoriteCities.add("Mumbai");
        updateFavoritesCombo();

        // Header
        add(createHeaderPanel(), BorderLayout.NORTH);

        // Center content
        JPanel center = new JPanel(new BorderLayout(12, 12));
        center.setOpaque(false);
        center.setBorder(new EmptyBorder(0, 12, 12, 12));
        center.add(createMainCard(), BorderLayout.CENTER);
        center.add(createDetailCard(), BorderLayout.EAST);
        add(center, BorderLayout.CENTER);

        // Bottom controls
        add(createBottomPanel(), BorderLayout.SOUTH);

        // Timer for auto refresh (30s)
        autoTimer = new javax.swing.Timer(30000, e -> performSearch(currentCity));

        // Listeners
        searchButton.addActionListener(e -> onSearch());
        searchField.addActionListener(e -> onSearch());
        
        addFavoriteButton.addActionListener(e -> addToFavorites());
        
        favoritesCombo.addActionListener(e -> {
            if (favoritesCombo.getSelectedItem() != null && !favoritesCombo.getSelectedItem().equals("Favorites")) {
                String city = (String) favoritesCombo.getSelectedItem();
                searchField.setText(city);
                performSearch(city);
            }
        });

        unitToggle.addActionListener(e -> {
            isCelsius = !isCelsius;
            unitToggle.setText(isCelsius ? "°F" : "°C");
            if (currentWeatherData != null) {
                updateUIWithData(currentWeatherData);
            }
        });

        autoButton.addActionListener(e -> {
            autoTimer.start();
            autoButton.setEnabled(false);
            stopAutoButton.setEnabled(true);
            showStatus("Auto-refresh enabled (every 30s)");
        });
        
        stopAutoButton.addActionListener(e -> {
            autoTimer.stop();
            autoButton.setEnabled(true);
            stopAutoButton.setEnabled(false);
            showStatus("Auto-refresh stopped");
        });

        // Start with default city
        searchField.setText(currentCity);
        performSearch(currentCity);
    }
    
    /**
     * FIX: The main method is the required entry point for a standalone Java application.
     */
    public static void main(String[] args) {
        // Schedule a job for the event-dispatching thread:
        // creating and showing this application's GUI.
        SwingUtilities.invokeLater(() -> {
            new WeatherSystem_API_Java().setVisible(true);
        });
    }

    private JPanel createHeaderPanel() {
        JPanel header = new JPanel(new BorderLayout(10, 10)) {
            @Override
            protected void paintComponent(Graphics g) {
                super.paintComponent(g);
                Graphics2D g2 = (Graphics2D) g;
                g2.setRenderingHint(RenderingHints.KEY_RENDERING, RenderingHints.VALUE_RENDER_QUALITY);
                int w = getWidth(), h = getHeight();
                GradientPaint gp = new GradientPaint(0, 0, new Color(15, 100, 180), w, h, new Color(50, 150, 230));
                g2.setPaint(gp);
                g2.fillRect(0, 0, w, h);
            }
        };
        header.setPreferredSize(new Dimension(10, 130));
        header.setBorder(new EmptyBorder(16, 18, 16, 18));

        // Title and subtitle
        JPanel titlePanel = new JPanel();
        titlePanel.setLayout(new BoxLayout(titlePanel, BoxLayout.Y_AXIS));
        titlePanel.setOpaque(false);
        
        JLabel title = new JLabel("Weather Station Pro");
        title.setFont(new Font("Segoe UI", Font.BOLD, 28));
        title.setForeground(Color.WHITE);
        
        JLabel subtitle = new JLabel("Real-time weather data powered by OpenWeatherMap");
        subtitle.setFont(new Font("Segoe UI", Font.PLAIN, 13));
        subtitle.setForeground(new Color(220, 235, 255));
        
        titlePanel.add(title);
        titlePanel.add(Box.createRigidArea(new Dimension(0, 4)));
        titlePanel.add(subtitle);

        // Search panel
        JPanel searchPanel = new JPanel(new FlowLayout(FlowLayout.RIGHT, 8, 0));
        searchPanel.setOpaque(false);
        
        JLabel favLabel = new JLabel("Quick:");
        favLabel.setForeground(Color.WHITE);
        favLabel.setFont(new Font("Segoe UI", Font.PLAIN, 12));
        
        favoritesCombo.setPreferredSize(new Dimension(120, 32));
        favoritesCombo.setFont(new Font("Segoe UI", Font.PLAIN, 12));
        
        searchField.setPreferredSize(new Dimension(250, 34));
        searchField.setFont(new Font("Segoe UI", Font.PLAIN, 14));
        searchField.setBorder(BorderFactory.createCompoundBorder(
            new LineBorder(new Color(200, 220, 240), 1, true),
            new EmptyBorder(4, 10, 4, 10)
        ));
        
        searchButton.setPreferredSize(new Dimension(80, 34));
        searchButton.setBackground(new Color(255, 255, 255));
        searchButton.setForeground(new Color(20, 100, 180));
        searchButton.setFont(new Font("Segoe UI", Font.BOLD, 13));
        searchButton.setFocusPainted(false);
        searchButton.setBorder(new EmptyBorder(6, 16, 6, 16));
        
        searchPanel.add(favLabel);
        searchPanel.add(favoritesCombo);
        searchPanel.add(Box.createRigidArea(new Dimension(12, 0)));
        searchPanel.add(searchField);
        searchPanel.add(searchButton);

        header.add(titlePanel, BorderLayout.WEST);
        header.add(searchPanel, BorderLayout.EAST);
        return header;
    }

    private JPanel createMainCard() {
        JPanel mainCard = new JPanel(new BorderLayout(12, 12));
        mainCard.setBorder(new EmptyBorder(20, 20, 20, 20));
        mainCard.setBackground(Color.WHITE);
        mainCard.setBorder(BorderFactory.createCompoundBorder(
            new LineBorder(new Color(220, 230, 240), 1, true),
            new EmptyBorder(20, 20, 20, 20)
        ));

        // Top section: City, Icon, Temp
        JPanel topSection = new JPanel(new BorderLayout(12, 12));
        topSection.setOpaque(false);
        
        cityLabel.setFont(new Font("Segoe UI", Font.BOLD, 26));
        cityLabel.setForeground(new Color(20, 80, 120));
        
        JPanel centerPanel = new JPanel(new FlowLayout(FlowLayout.CENTER, 20, 10));
        centerPanel.setOpaque(false);
        
        mainIcon.setFont(new Font("Segoe UI Emoji", Font.PLAIN, 80));
        mainIcon.setPreferredSize(new Dimension(120, 120));
        
        JPanel tempPanel = new JPanel();
        tempPanel.setLayout(new BoxLayout(tempPanel, BoxLayout.Y_AXIS));
        tempPanel.setOpaque(false);
        
        tempLabel.setFont(new Font("Segoe UI Light", Font.BOLD, 56));
        tempLabel.setForeground(new Color(230, 80, 50));
        tempLabel.setAlignmentX(Component.CENTER_ALIGNMENT);
        
        feelsLikeLabel.setFont(new Font("Segoe UI", Font.PLAIN, 14));
        feelsLikeLabel.setForeground(new Color(100, 100, 120));
        feelsLikeLabel.setAlignmentX(Component.CENTER_ALIGNMENT);
        
        descLabel.setFont(new Font("Segoe UI", Font.PLAIN, 18));
        descLabel.setForeground(new Color(60, 60, 80));
        descLabel.setAlignmentX(Component.CENTER_ALIGNMENT);
        
        tempPanel.add(tempLabel);
        tempPanel.add(Box.createRigidArea(new Dimension(0, 4)));
        tempPanel.add(feelsLikeLabel);
        tempPanel.add(Box.createRigidArea(new Dimension(0, 8)));
        tempPanel.add(descLabel);
        
        centerPanel.add(mainIcon);
        centerPanel.add(tempPanel);
        
        topSection.add(cityLabel, BorderLayout.NORTH);
        topSection.add(centerPanel, BorderLayout.CENTER);

        // Stats grid
        JPanel statsGrid = new JPanel(new GridLayout(2, 2, 12, 12));
        statsGrid.setOpaque(false);
        statsGrid.setBorder(new EmptyBorder(20, 0, 20, 0));
        
        statsGrid.add(makeStatCard("💧 Humidity", humidityLabel, new Color(80, 150, 220)));
        statsGrid.add(makeStatCard("💨 Wind Speed", windLabel, new Color(120, 180, 240)));
        statsGrid.add(makeStatCard("🔽 Pressure", pressureLabel, new Color(180, 140, 220)));
        statsGrid.add(makeStatCard("👁 Visibility", visibilityLabel, new Color(100, 200, 150)));

        // Forecast section
        JPanel forecastSection = new JPanel(new BorderLayout(8, 8));
        forecastSection.setOpaque(false);
        forecastSection.setBorder(new EmptyBorder(12, 0, 0, 0));
        
        JLabel forecastTitle = new JLabel("5-Day Forecast");
        forecastTitle.setFont(new Font("Segoe UI Semibold", Font.BOLD, 16));
        forecastTitle.setForeground(new Color(40, 60, 80));
        
        forecastPanel.setOpaque(false);
        
        forecastSection.add(forecastTitle, BorderLayout.NORTH);
        forecastSection.add(forecastPanel, BorderLayout.CENTER);

        mainCard.add(topSection, BorderLayout.NORTH);
        mainCard.add(statsGrid, BorderLayout.CENTER);
        mainCard.add(forecastSection, BorderLayout.SOUTH);

        return mainCard;
    }

    private JPanel createDetailCard() {
        JPanel detail = new JPanel();
        detail.setPreferredSize(new Dimension(220, 400));
        detail.setLayout(new BoxLayout(detail, BoxLayout.Y_AXIS));
        detail.setBorder(BorderFactory.createCompoundBorder(
            new LineBorder(new Color(220, 230, 240), 1, true),
            new EmptyBorder(20, 18, 20, 18)
        ));
        detail.setBackground(Color.WHITE);

        JLabel header = new JLabel("Additional Info");
        header.setFont(new Font("Segoe UI Semibold", Font.BOLD, 18));
        header.setForeground(new Color(40, 60, 80));
        header.setAlignmentX(Component.LEFT_ALIGNMENT);
        
        detail.add(header);
        detail.add(Box.createRigidArea(new Dimension(0, 20)));

        detail.add(makeInfoRow("🌅 Sunrise", sunriseLabel));
        detail.add(Box.createRigidArea(new Dimension(0, 12)));
        detail.add(makeInfoRow("🌇 Sunset", sunsetLabel));
        detail.add(Box.createRigidArea(new Dimension(0, 24)));
        
        JPanel unitPanel = new JPanel(new FlowLayout(FlowLayout.LEFT, 0, 0));
        unitPanel.setOpaque(false);
        unitPanel.setAlignmentX(Component.LEFT_ALIGNMENT);
        unitPanel.setMaximumSize(new Dimension(300, 40));
        
        JLabel unitLabel = new JLabel("Temperature Unit:");
        unitLabel.setFont(new Font("Segoe UI", Font.PLAIN, 12));
        unitLabel.setForeground(new Color(80, 80, 100));
        
        unitToggle.setPreferredSize(new Dimension(50, 28));
        unitToggle.setFont(new Font("Segoe UI", Font.BOLD, 11));
        unitToggle.setFocusPainted(false);
        
        unitPanel.add(unitLabel);
        unitPanel.add(Box.createRigidArea(new Dimension(8, 0)));
        unitPanel.add(unitToggle);
        
        detail.add(unitPanel);
        detail.add(Box.createRigidArea(new Dimension(0, 16)));
        
        addFavoriteButton.setAlignmentX(Component.LEFT_ALIGNMENT);
        addFavoriteButton.setMaximumSize(new Dimension(300, 36));
        addFavoriteButton.setBackground(new Color(255, 200, 100));
        addFavoriteButton.setForeground(new Color(60, 40, 20));
        addFavoriteButton.setFont(new Font("Segoe UI", Font.BOLD, 12));
        addFavoriteButton.setFocusPainted(false);
        
        detail.add(addFavoriteButton);
        detail.add(Box.createVerticalGlue());
        
        return detail;
    }

    private JPanel createBottomPanel() {
        JPanel bottom = new JPanel(new FlowLayout(FlowLayout.CENTER, 16, 12));
        bottom.setOpaque(false);
        bottom.setBorder(new EmptyBorder(0, 12, 12, 12));
        
        styleButton(autoButton, new Color(50, 150, 90), Color.WHITE);
        styleButton(stopAutoButton, new Color(200, 60, 60), Color.WHITE);
        
        stopAutoButton.setEnabled(false);
        
        updatedLabel.setFont(new Font("Segoe UI", Font.PLAIN, 12));
        updatedLabel.setForeground(new Color(100, 100, 120));
        
        bottom.add(autoButton);
        bottom.add(stopAutoButton);
        bottom.add(Box.createRigidArea(new Dimension(20, 0)));
        bottom.add(updatedLabel);
        
        return bottom;
    }

    private JPanel makeStatCard(String title, JLabel valueLabel, Color accentColor) {
        JPanel card = new JPanel(new BorderLayout(8, 8));
        card.setBackground(new Color(248, 250, 252));
        card.setBorder(BorderFactory.createCompoundBorder(
            new LineBorder(new Color(230, 235, 245), 1, true),
            new EmptyBorder(12, 14, 12, 14)
        ));
        
        JLabel titleLabel = new JLabel(title);
        titleLabel.setFont(new Font("Segoe UI", Font.PLAIN, 13));
        titleLabel.setForeground(new Color(80, 90, 110));
        
        valueLabel.setFont(new Font("Segoe UI", Font.BOLD, 20));
        valueLabel.setForeground(accentColor);
        
        card.add(titleLabel, BorderLayout.NORTH);
        card.add(valueLabel, BorderLayout.CENTER);
        
        return card;
    }

    private JPanel makeInfoRow(String label, JLabel value) {
        JPanel row = new JPanel(new BorderLayout(8, 4));
        row.setOpaque(false);
        row.setAlignmentX(Component.LEFT_ALIGNMENT);
        row.setMaximumSize(new Dimension(300, 40));
        
        JLabel l = new JLabel(label);
        l.setFont(new Font("Segoe UI", Font.PLAIN, 13));
        l.setForeground(new Color(80, 90, 110));
        
        value.setFont(new Font("Segoe UI", Font.BOLD, 14));
        value.setForeground(new Color(40, 60, 80));
        
        row.add(l, BorderLayout.WEST);
        row.add(value, BorderLayout.EAST);
        
        return row;
    }

    private void styleButton(JButton btn, Color bg, Color fg) {
        btn.setBackground(bg);
        btn.setForeground(fg);
        btn.setFont(new Font("Segoe UI", Font.BOLD, 12));
        btn.setFocusPainted(false);
        btn.setBorder(new EmptyBorder(8, 16, 8, 16));
    }

    private void onSearch() {
        String q = searchField.getText().trim();
        if (q.isEmpty()) {
            JOptionPane.showMessageDialog(this, "Please enter a city name.", 
                "Input Required", JOptionPane.WARNING_MESSAGE);
            return;
        }
        // Do NOT update currentCity yet, only after a successful API call
        performSearch(q);
    }

    private void addToFavorites() {
        if (currentCity != null && !currentCity.isEmpty()) {
            // Use the internally stored currentCity, which is capitalized on successful fetch
            if (favoriteCities.add(currentCity)) { 
                updateFavoritesCombo();
                showStatus("Added " + currentCity + " to favorites");
            } else {
                showStatus(currentCity + " is already in favorites");
            }
        }
    }

    private void updateFavoritesCombo() {
        String selected = (String) favoritesCombo.getSelectedItem();
        favoritesCombo.removeAllItems();
        favoritesCombo.addItem("Favorites");
        for (String city : favoriteCities) {
            favoritesCombo.addItem(city);
        }
        if (selected != null && favoriteCities.contains(selected)) {
            favoritesCombo.setSelectedItem(selected);
        } else {
            favoritesCombo.setSelectedIndex(0);
        }
    }

    private void showStatus(String message) {
        updatedLabel.setText(message);
    }

    private void performSearch(String city) {
        String queryCity = capitalize(city); // Capitalize for display while loading
        
        cityLabel.setText("Loading " + queryCity + "...");
        tempLabel.setText("--°");
        mainIcon.setText("⏳");
        descLabel.setText("Fetching data...");
        feelsLikeLabel.setText("Please wait");
        
        // Use SwingWorker to perform network operations in the background
        new SwingWorker<WeatherData, Void>() {
            protected WeatherData doInBackground() {
                return station.fetchWeatherFromAPI(city);
            }
            
            protected void done() {
                try {
                    WeatherData data = get();
                    if (data == null) {
                        // Check if the error was due to the API Key being missing
                        if (station.isApiKeyMissing()) {
                            showError("API Key Missing! Please replace \"YOUR_API_KEY\" in the code.");
                        } else {
                            // City not found or general API failure
                            showError("Unable to fetch weather data for " + queryCity + ". Please check the city name.");
                        }
                        return;
                    }
                    
                    // --- FIX: Only update currentCity and searchField on SUCCESS ---
                    currentWeatherData = data;
                    currentCity = queryCity; // Store the successfully fetched and capitalized city name
                    searchField.setText(currentCity); // Update search field with the clean name
                    // -------------------------------------------------------------
                    
                    updateUIWithData(data);
                    
                    // Fetch 5-day forecast
                    fetchForecast(currentCity); // Use the clean/validated city name
                    
                } catch (Exception ex) {
                    showError("Error: " + ex.getMessage());
                }
            }
        }.execute();
    }

    private void updateUIWithData(WeatherData data) {
        cityLabel.setText(currentCity); // Use the currentCity variable, now set from successful fetch
        
        double temp = isCelsius ? data.temperature : celsiusToFahrenheit(data.temperature);
        double feelsLike = isCelsius ? data.feelsLike : celsiusToFahrenheit(data.feelsLike);
        String unit = isCelsius ? "°C" : "°F";
        
        tempLabel.setText(String.format("%.0f%s", temp, unit));
        feelsLikeLabel.setText(String.format("Feels like: %.0f%s", feelsLike, unit));
        descLabel.setText(capitalize(data.description));
        mainIcon.setText(getEmojiForWeatherCode(data.weatherId));
        
        humidityLabel.setText(String.format("%.0f%%", data.humidity));
        windLabel.setText(String.format("%.1f m/s", data.windSpeed));
        pressureLabel.setText(String.format("%.0f hPa", data.pressure));
        visibilityLabel.setText(String.format("%.1f km", data.visibility / 1000.0));
        
        sunriseLabel.setText(data.sunrise);
        sunsetLabel.setText(data.sunset);
        
        updatedLabel.setText("Last updated: " + new SimpleDateFormat("hh:mm:ss a").format(new Date()));
    }

    private void fetchForecast(String city) {
        new SwingWorker<List<ForecastData>, Void>() {
            protected List<ForecastData> doInBackground() {
                return station.fetch5DayForecast(city);
            }
            
            protected void done() {
                try {
                    List<ForecastData> forecast = get();
                    if (forecast != null && !forecast.isEmpty()) {
                        updateForecastUI(forecast);
                    }
                } catch (Exception ex) {
                    System.err.println("Forecast error: " + ex.getMessage());
                }
            }
        }.execute();
    }

    private void updateForecastUI(List<ForecastData> forecast) {
        forecastPanel.removeAll();
        
        for (int i = 0; i < Math.min(5, forecast.size()); i++) {
            ForecastData f = forecast.get(i);
            JPanel dayCard = new JPanel();
            dayCard.setLayout(new BoxLayout(dayCard, BoxLayout.Y_AXIS));
            dayCard.setBackground(new Color(248, 250, 255));
            dayCard.setBorder(BorderFactory.createCompoundBorder(
                new LineBorder(new Color(220, 230, 245), 1, true),
                new EmptyBorder(8, 6, 8, 6)
            ));
            
            JLabel dayLabel = new JLabel(f.day, SwingConstants.CENTER);
            dayLabel.setFont(new Font("Segoe UI", Font.BOLD, 11));
            dayLabel.setAlignmentX(Component.CENTER_ALIGNMENT);
            
            JLabel icon = new JLabel(getEmojiForWeatherCode(f.weatherId), SwingConstants.CENTER);
            icon.setFont(new Font("Segoe UI Emoji", Font.PLAIN, 28));
            icon.setAlignmentX(Component.CENTER_ALIGNMENT);
            
            double temp = isCelsius ? f.temp : celsiusToFahrenheit(f.temp);
            String unit = isCelsius ? "°C" : "°F";
            JLabel tempLabel = new JLabel(String.format("%.0f%s", temp, unit), SwingConstants.CENTER);
            tempLabel.setFont(new Font("Segoe UI", Font.BOLD, 13));
            tempLabel.setForeground(new Color(230, 80, 50));
            tempLabel.setAlignmentX(Component.CENTER_ALIGNMENT);
            
            dayCard.add(dayLabel);
            dayCard.add(Box.createRigidArea(new Dimension(0, 4)));
            dayCard.add(icon);
            dayCard.add(Box.createRigidArea(new Dimension(0, 4)));
            dayCard.add(tempLabel);
            
            forecastPanel.add(dayCard);
        }
        
        forecastPanel.revalidate();
        forecastPanel.repaint();
    }

    private void showError(String message) {
        cityLabel.setText("Error");
        tempLabel.setText("--°");
        mainIcon.setText("❌");
        descLabel.setText(message);
        feelsLikeLabel.setText("");
        humidityLabel.setText("-- %");
        windLabel.setText("-- m/s");
        pressureLabel.setText("-- hPa");
        visibilityLabel.setText("-- km");
        sunriseLabel.setText("--");
        sunsetLabel.setText("--");
        updatedLabel.setText("Update failed");
        
        // Clear forecast on error
        forecastPanel.removeAll();
        forecastPanel.revalidate();
        forecastPanel.repaint();
    }

    private double celsiusToFahrenheit(double celsius) {
        return celsius * 9.0 / 5.0 + 32.0;
    }

    private static String capitalize(String s) {
        if (s == null || s.isEmpty()) return s;
        String[] words = s.split(" ");
        StringBuilder result = new StringBuilder();
        for (String word : words) {
            if (!word.isEmpty()) {
                result.append(Character.toUpperCase(word.charAt(0)))
                      .append(word.substring(1).toLowerCase())
                      .append(" ");
            }
        }
        return result.toString().trim();
    }

    private String getEmojiForWeatherCode(int code) {
        if (code >= 200 && code < 300) return "⛈️";  // Thunderstorm
        if (code >= 300 && code < 400) return "🌦️";  // Drizzle
        if (code >= 500 && code < 600) return "🌧️";  // Rain
        if (code >= 600 && code < 700) return "❄️";  // Snow
        if (code >= 700 && code < 800) return "🌫️";  // Atmosphere (fog, mist, etc.)
        if (code == 800) return "☀️";                // Clear
        if (code == 801) return "🌤️";               // Few clouds
        if (code == 802) return "⛅";                // Scattered clouds
        if (code >= 803) return "☁️";                // Overcast
        return "🌡️";
    }

    // -------------------- Data Models --------------------
    static class WeatherData {
        double temperature, feelsLike, humidity, windSpeed, pressure, visibility;
        int weatherId;
        String description, sunrise, sunset;
        
        public WeatherData(double temperature, double feelsLike, double humidity, 
                          double windSpeed, double pressure, double visibility,
                          int weatherId, String description, String sunrise, String sunset) {
            this.temperature = temperature;
            this.feelsLike = feelsLike;
            this.humidity = humidity;
            this.windSpeed = windSpeed;
            this.pressure = pressure;
            this.visibility = visibility;
            this.weatherId = weatherId;
            this.description = description;
            this.sunrise = sunrise;
            this.sunset = sunset;
        }
    }

    static class ForecastData {
        String day;
        double temp;
        int weatherId;
        
        public ForecastData(String day, double temp, int weatherId) {
            this.day = day;
            this.temp = temp;
            this.weatherId = weatherId;
        }
    }

    // -------------------- Weather API --------------------
    class WeatherStation {
        // !!! IMPORTANT: The API key has been replaced with the key you provided. 
        // If the error persists, please wait 1-2 hours for key activation or check your network.
        private final String API_KEY = "cc9bf6c8822ed2764ad84ec36d5823e4"; // <--- LINE 663: INSERTED YOUR NEW KEY

        public boolean isApiKeyMissing() {
            return API_KEY.equals("YOUR_API_KEY");
        }

        public WeatherData fetchWeatherFromAPI(String city) {
            try {
                if (isApiKeyMissing()) {
                    return null; // Return null so the calling SwingWorker can show the missing key error
                }
                
                String q = URLEncoder.encode(city, "UTF-8");
                String api = "https://api.openweathermap.org/data/2.5/weather?q=" + q + 
                            "&appid=" + API_KEY + "&units=metric";
                
                JSONObject obj = fetchJSON(api);
                if (obj == null) return null;

                // Check for city not found error (OpenWeatherMap usually returns code 404)
                if (obj.has("cod") && obj.optInt("cod") == 404) {
                     return null;
                }

                JSONObject main = obj.getJSONObject("main");
                JSONObject wind = obj.optJSONObject("wind");
                JSONObject sys = obj.optJSONObject("sys");
                JSONArray weather = obj.optJSONArray("weather");

                double temp = main.optDouble("temp", 0);
                double feelsLike = main.optDouble("feels_like", temp);
                double humidity = main.optDouble("humidity", 0);
                double pressure = main.optDouble("pressure", 0);
                double windSpeed = wind != null ? wind.optDouble("speed", 0) : 0;
                double visibility = obj.optDouble("visibility", 0);
                
                int weatherId = 800;
                String description = "Unknown";
                if (weather != null && weather.length() > 0) {
                    JSONObject w = weather.getJSONObject(0);
                    weatherId = w.optInt("id", 800);
                    description = w.optString("description", "Unknown");
                }
                
                String sunrise = "--";
                String sunset = "--";
                if (sys != null) {
                    sunrise = convertUnixToTime(sys.optLong("sunrise", 0));
                    sunset = convertUnixToTime(sys.optLong("sunset", 0));
                }

                return new WeatherData(temp, feelsLike, humidity, windSpeed, pressure, visibility,
                                       weatherId, description, sunrise, sunset);

            } catch (Exception e) {
                System.err.println("API Fetch Error: " + e.getMessage());
                return null;
            }
        }
        
        public List<ForecastData> fetch5DayForecast(String city) {
            List<ForecastData> forecastList = new ArrayList<>();
            try {
                if (isApiKeyMissing()) {
                    return Collections.emptyList();
                }
                
                String q = URLEncoder.encode(city, "UTF-8");
                String api = "https://api.openweathermap.org/data/2.5/forecast?q=" + q + 
                            "&appid=" + API_KEY + "&units=metric";

                JSONObject obj = fetchJSON(api);
                if (obj == null || obj.has("cod") && obj.optInt("cod") != 200) return forecastList;
                
                JSONArray list = obj.getJSONArray("list");
                
                // Use a map to track and select one forecast point per day (around noon is preferred)
                Map<String, JSONObject> dayMap = new LinkedHashMap<>();
                SimpleDateFormat dayFormat = new SimpleDateFormat("EEE"); // Day of the week (e.g., Mon)

                for (int i = 0; i < list.length(); i++) {
                    JSONObject item = list.getJSONObject(i);
                    long dt = item.getLong("dt");
                    String day = dayFormat.format(new Date(dt * 1000));
                    
                    // Only consider if the day hasn't been added yet,
                    // or if it's the 12:00:00 forecast (for a good representative temperature)
                    if (!dayMap.containsKey(day) || item.getString("dt_txt").contains("12:00:00")) {
                        dayMap.put(day, item);
                    }
                }
                
                // Convert the map entries to ForecastData
                for (Map.Entry<String, JSONObject> entry : dayMap.entrySet()) {
                    JSONObject item = entry.getValue();
                    JSONObject main = item.getJSONObject("main");
                    JSONArray weather = item.getJSONArray("weather");
                    
                    double temp = main.optDouble("temp", 0);
                    int weatherId = 800;
                    if (weather.length() > 0) {
                        weatherId = weather.getJSONObject(0).optInt("id", 800);
                    }
                    
                    forecastList.add(new ForecastData(entry.getKey(), temp, weatherId));
                }
                
                // Ensure we return up to 5 distinct days (if available)
                // If the first item is today, we only want the next 5 days.
                if (forecastList.size() > 5) {
                    // This handles the case where the forecast includes the current day.
                    return forecastList.subList(1, 6); 
                } else {
                    return forecastList;
                }
                
            } catch (Exception e) {
                System.err.println("Forecast Fetch Error: " + e.getMessage());
            }
            return Collections.emptyList();
        }

        private JSONObject fetchJSON(String urlString) throws IOException, JSONException {
            URL url = new URL(urlString);
            HttpURLConnection conn = (HttpURLConnection) url.openConnection();
            conn.setRequestMethod("GET");
            conn.connect();

            int responseCode = conn.getResponseCode();
            if (responseCode != 200) {
                // Read the error stream for detailed messages
                try (BufferedReader reader = new BufferedReader(new InputStreamReader(conn.getErrorStream()))) {
                    StringBuilder errorResponse = new StringBuilder();
                    String line;
                    while ((line = reader.readLine()) != null) {
                        errorResponse.append(line);
                    }
                    if (errorResponse.length() > 0) {
                         System.err.println("API Error Response: " + errorResponse.toString());
                         // Try to parse error as JSON to check for 404
                         try {
                             return new JSONObject(errorResponse.toString());
                         } catch (JSONException ignored) {
                             // Ignore if not JSON
                         }
                    }
                }
                throw new IOException("HTTP response code: " + responseCode);
            }

            try (BufferedReader reader = new BufferedReader(new InputStreamReader(conn.getInputStream()))) {
                StringBuilder response = new StringBuilder();
                String line;
                while ((line = reader.readLine()) != null) {
                    response.append(line);
                }
                return new JSONObject(response.toString());
            }
        }
        
        private String convertUnixToTime(long unixSeconds) {
            if (unixSeconds == 0) return "--";
            Date date = new Date(unixSeconds * 1000L);
            SimpleDateFormat sdf = new SimpleDateFormat("hh:mm a");
            return sdf.format(date);
        }
    }
}