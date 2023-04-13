import javafx.application.Application;
import javafx.beans.property.ObjectProperty;
import javafx.beans.value.ChangeListener;
import javafx.beans.value.ObservableValue;
import javafx.concurrent.Service;
import javafx.concurrent.Task;
import javafx.geometry.Pos;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.*;
import javafx.scene.text.Font;
import javafx.scene.text.Text;
import javafx.stage.Stage;

import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.net.URLEncoder;
import java.util.ArrayList;
import java.util.List;
import java.util.Scanner;

import org.json.JSONArray;
import org.json.JSONObject;

public class RecipeGenerator extends Application {
    //API Information for Edamam access
    private final String APP_ID = "77b2c7fb";
    private final String APP_KEY = "cab0ebeed12d81e0f0b59f2d03f16e75";
    private final String API_URL = "https://api.edamam.com/search?q=%s&app_id=%s&app_key=%s";
    private final String DEFAULT_IMAGE_URL = "https://via.placeholder.com/150x150.png?text=No+Image";
    //Declares variables for displaying the recipe list and search bar for the API
    private ListView<Recipe> recipeListView;
    private Service<List<Recipe>> searchService;

    public void start(Stage primaryStage) {
        primaryStage.setTitle("Recipe Search");

        // Creates a search bar with a button
        TextField searchField = new TextField();
        searchField.setPromptText("Search for a recipe");
        Button searchButton = new Button("Search");
        HBox searchBox = new HBox(searchField, searchButton);
        searchBox.setSpacing(10);
        searchBox.setAlignment(Pos.CENTER);

        // Create filter controls
        CheckBox vegetarianCheckbox = new CheckBox("Vegetarian");
        CheckBox veganCheckbox = new CheckBox("Vegan");
        CheckBox glutenFreeCheckbox = new CheckBox("Gluten-free");
        CheckBox dairyFreeCheckbox = new CheckBox("Dairy-free");
        CheckBox peanutFreeCheckbox = new CheckBox("Peanut-free");
        CheckBox treeNutFreeCheckbox = new CheckBox("Tree Nut-free");
        CheckBox eggFreeCheckbox = new CheckBox("Egg-free");
        CheckBox soyFreeCheckbox = new CheckBox("Soy-free");
        CheckBox fishFreeCheckbox = new CheckBox("Fish-free");
        HBox filterBox = new HBox(vegetarianCheckbox, veganCheckbox, glutenFreeCheckbox, dairyFreeCheckbox, peanutFreeCheckbox, treeNutFreeCheckbox, eggFreeCheckbox, soyFreeCheckbox, fishFreeCheckbox);
        filterBox.setSpacing(10);
        filterBox.setAlignment(Pos.CENTER);

        // Create list view to display recipe results 
        recipeListView = new ListView<>();
        recipeListView.setCellFactory(recipeListView -> new RecipeCell());
        recipeListView.setPlaceholder(new Text("No recipes found"));
        recipeListView.getSelectionModel().selectedItemProperty().addListener(new ChangeListener<Recipe>() {
            
            // Changes recipe based on selection
            public void changed(ObservableValue<? extends Recipe> observable, Recipe oldValue, Recipe newValue) {
                if (newValue != null) {
                    showRecipeDetails(newValue);
                }
            }
        });

        // Create main layout
        VBox layout = new VBox(searchBox, filterBox, recipeListView);
        layout.setSpacing(10);
        layout.setAlignment(Pos.CENTER);

        // Create search button action
        searchButton.setOnAction(event -> {
            searchService.restart();
        });
    
        // Create search service for searching the recipes
        searchService = new Service<List<Recipe>>() {
            
            protected Task<List<Recipe>> createTask() {
                return new Task<List<Recipe>>() {
                    
                    protected List<Recipe> call() throws Exception {
                        return searchRecipes(searchField.getText(), vegetarianCheckbox.isSelected(), veganCheckbox.isSelected(),
                                glutenFreeCheckbox.isSelected(), dairyFreeCheckbox.isSelected(), peanutFreeCheckbox.isSelected(),
                                treeNutFreeCheckbox.isSelected(), eggFreeCheckbox.isSelected(), soyFreeCheckbox.isSelected(),
                                fishFreeCheckbox.isSelected());
                    }
                };
            }
        };
        searchService.setOnSucceeded(event -> {
            List<Recipe> recipes = searchService.getValue();
            recipeListView.getItems().setAll(recipes);
        });
        searchService.setOnFailed(event -> {
            Alert alert = new Alert(Alert.AlertType.ERROR, "Failed to search for recipes: " + searchService.getException().getMessage());
            alert.showAndWait();
        });
    
        // Create scene and show stage
        Scene scene = new Scene(layout, 800, 600);
        primaryStage.setScene(scene);
        primaryStage.show();
    }
    

    private List<Recipe> searchRecipes(String searchTerm, boolean vegetarian, boolean vegan, boolean glutenFree, boolean dairyFree,
                                       boolean peanutFree, boolean treeNutFree, boolean eggFree, boolean soyFree, boolean fishFree) {
        List<Recipe> recipes = new ArrayList<>();
    
        try {
            // Create API URL with search term and API key
            String apiUrl = String.format(API_URL, URLEncoder.encode(searchTerm, "UTF-8"), APP_ID, APP_KEY);
    
            // Add filters to the API URL
            if (vegetarian) {
                apiUrl += "&health=vegetarian";
            }
            if (vegan) {
                apiUrl += "&health=vegan";
            }
            if (glutenFree) {
                apiUrl += "&health=gluten-free";
            }
            if (dairyFree) {
                apiUrl += "&health=dairy-free";
            }
            if (peanutFree) {
                apiUrl += "&health=peanut-free";
            }
            if (treeNutFree) {
                apiUrl += "&health=tree-nut-free";
            }
            if (eggFree) {
                apiUrl += "&health=egg-free";
            }
            if (soyFree) {
                apiUrl += "&health=soy-free";
            }
            if (fishFree) {
                apiUrl += "&health=fish-free";
            }
    
            // Calls API to search for recipes to Edamam
            JSONObject response = new JSONObject(readUrl(apiUrl));
            JSONArray hits = response.getJSONArray("hits");
            for (int i = 0; i < hits.length(); i++) {
                JSONObject hit = hits.getJSONObject(i);
                JSONObject recipeObj = hit.getJSONObject("recipe");
                Recipe recipe = new Recipe();
                recipe.setTitle(recipeObj.getString("label"));
                recipe.setImageUrl(recipeObj.optString("image", DEFAULT_IMAGE_URL));
                JSONArray ingredients = recipeObj.getJSONArray("ingredientLines");
                List<String> ingredientList = new ArrayList<>();
                for (int j = 0; j < ingredients.length(); j++) {
                    ingredientList.add(ingredients.getString(j));
                }
                recipe.setIngredients(ingredientList);
                recipes.add(recipe);
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    
        return recipes;
    }
    
    
    private void showRecipeDetails(Recipe recipe) {
        // Create recipe details 
        Dialog<Void> dialog = new Dialog<>();
        dialog.setTitle(recipe.getTitle());
        dialog.setResizable(true);
        dialog.getDialogPane().setPrefSize(500, 600);
    
        // Create image view for the recipe image
        ImageView imageView = new ImageView();
        imageView.setFitWidth(150);
        imageView.setFitHeight(150);
        imageView.setPreserveRatio(true);
        imageView.setSmooth(true);
        imageView.setCache(true);
        loadImageAsync(recipe.getImageUrl(), imageView.imageProperty());
        
        // Create text area for the ingredients
        TextArea ingredientsTextArea = new TextArea();
        ingredientsTextArea.setWrapText(true);
        ingredientsTextArea.setText(String.join("\n", recipe.getIngredients()));
    
        // Create layout for recipe details
        VBox recipeLayout = new VBox(imageView, new Label("Ingredients"), ingredientsTextArea);
        dialog.getDialogPane().setContent(recipeLayout);
    
        // Create close button
        Button closeButton = new Button("Close");
        closeButton.setOnAction(event -> {
            dialog.close();
        });
    
        // Add close button to bottom of dialog
        dialog.getDialogPane().getButtonTypes().addAll(ButtonType.CLOSE);
        dialog.getDialogPane().lookupButton(ButtonType.CLOSE).setVisible(false);
        dialog.getDialogPane().getChildren().add(closeButton);
    
        // Show dialog
        dialog.showAndWait();
    }
    
    
    private static void loadImageAsync(String imageUrl, ObjectProperty<Image> imageProperty) {
        Service<Image> imageService = new Service<Image>() {
            
            protected Task<Image> createTask() {
                return new Task<Image>() {
                    
                    protected Image call() throws Exception {
                        try (InputStream in = new URL(imageUrl).openStream()) {
                            return new Image(in);
                        }
                    }
                };
            }
        };
        imageService.setOnSucceeded(event -> {
            Image image = imageService.getValue();
            if (image != null) {
                imageProperty.set(image);
            }
        });
        imageService.start();
    }
    
    private String readUrl(String urlString) throws IOException {
        try (Scanner scanner = new Scanner(new URL(urlString).openStream(), "UTF-8").useDelimiter("\\A")) {
            return scanner.hasNext() ? scanner.next() : "";
        }
    }
    
    private static class Recipe {
        private String title;
        private String imageUrl;
        private List<String> ingredients;
    
        public Recipe() {
            this.ingredients = new ArrayList<>();
        }
    
        public String getTitle() {
            return title;
        }
    
        public void setTitle(String title) {
            this.title = title;
        }
    
        public String getImageUrl() {
            return imageUrl;
        }
    
        public void setImageUrl(String imageUrl) {
            this.imageUrl = imageUrl;
        }
    
        public List<String> getIngredients() {
            return ingredients;
        }
    
        public void setIngredients(List<String> ingredients) {
            this.ingredients = ingredients;
        }
    
        
        public String toString() {
            return title;
    
        }
    }
    
    private static class RecipeCell extends ListCell<Recipe> {
        private HBox hbox;
        private ImageView imageView;
        private Text text;
    
        //will display the recipes with images set
        public RecipeCell() {
            super();
    
            imageView = new ImageView();
            imageView.setFitWidth(50);
            imageView.setFitHeight(50);
            imageView.setPreserveRatio(true);
            imageView.setSmooth(true);
            imageView.setCache(true);
    
            text = new Text();
            text.setFont(Font.font("arial", 14));
    
            hbox = new HBox(imageView, text);
            hbox.setSpacing(10);
            hbox.setAlignment(Pos.CENTER_LEFT);
        }
    
        
        protected void updateItem(Recipe recipe, boolean empty) {
            super.updateItem(recipe, empty);
            if (empty || recipe == null) {
                setGraphic(null);
            } else {
                text.setText(recipe.getTitle());
                loadImageAsync(recipe.getImageUrl(), imageView.imageProperty());
                setGraphic(hbox);
            }
        }
    }

    //Launches
    public static void main(String[] args) {
        launch(args);
    }
}