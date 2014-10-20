package de.htw.sdf.photoplatform.webservice;

import java.io.IOException;
import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.bind.annotation.ResponseStatus;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

import de.htw.sdf.photoplatform.exception.NotFoundException;
import de.htw.sdf.photoplatform.manager.RecipeManager;
import de.htw.sdf.photoplatform.persistence.Ingredient;
import de.htw.sdf.photoplatform.persistence.Recipe;
import de.htw.sdf.photoplatform.persistence.RecipeDifficulty;
import de.htw.sdf.photoplatform.persistence.Unit;
import de.htw.sdf.photoplatform.persistence.UsedIngredient;
import de.htw.sdf.photoplatform.webservice.common.BaseAPIController;
import de.htw.sdf.photoplatform.webservice.common.Endpoints;

@Controller
public class RecipeController extends BaseAPIController {

	@Autowired
	RecipeManager recipeManager;

	@RequestMapping(value = Endpoints.RECIPE_BY_NAME, method = RequestMethod.GET)
	@ResponseBody
	public Recipe recipeByName(@PathVariable String name) {
		Recipe recipe = recipeManager.findByName(name);
		return recipe;
	}
	
	@RequestMapping(value = Endpoints.RECIPE_BY_ID, method = RequestMethod.GET)
	@ResponseBody
	public Recipe recipeById(@PathVariable int id) {
		Recipe recipe = recipeManager.findById(id);
		return recipe;
	}

	@RequestMapping(value = Endpoints.RECIPE_BY_USERNAME, method = RequestMethod.GET)
	@ResponseBody
	public List<Recipe> recipeByUserName(@PathVariable String name) {
		return recipeManager.findByUserName(name);
	}

	@RequestMapping(value = Endpoints.RECIPE_ALL, method = RequestMethod.GET)
	@ResponseBody
	public List<Recipe> retrieveAllRecipes() throws NotFoundException {
		List<Recipe> recipeList = recipeManager.findAll();
		if (recipeList == null)
			throw new NotFoundException("No recipes found!");
		else
			return recipeList;
	}

	@RequestMapping(value = Endpoints.RECIPE_DELETE_BY_NAME, method = RequestMethod.GET)
	@ResponseBody
	public boolean deleteRecipeByName(@PathVariable String name) {
		Recipe recipeToDelete = recipeManager.findByName(name);
		recipeManager.delete(recipeToDelete);
		return true;
	}

	@RequestMapping(value = Endpoints.RECIPE_CREATE, method = RequestMethod.POST)
	@ResponseBody
	@ResponseStatus(HttpStatus.CREATED)
	public Recipe createRecipe(@RequestBody Recipe recipe) {
		recipeManager.create(recipe);
		return recipe;
	}

	@RequestMapping(value = Endpoints.RECIPE_UPDATE, method = RequestMethod.PUT)
	@ResponseBody
	@ResponseStatus(HttpStatus.OK)
	public boolean updateRecipe(@PathVariable long id,
			@RequestBody Recipe recipe) {
		recipeManager.update(recipe);
		return true;
	}

	@RequestMapping(value = Endpoints.RECIPE_DIFFICULTY_ALL, method = RequestMethod.GET)
	@ResponseBody
	public List<RecipeDifficulty> retrieveAllRecipeDifficulties()
			throws Exception {
		List<RecipeDifficulty> recipeDifficultyList = recipeManager
				.findAllRecipeDifficulties();
		if (recipeDifficultyList == null)
			throw new Exception("No difficulties found!");
		else
			return recipeDifficultyList;
	}

	@RequestMapping(value = Endpoints.RECIPE_DELETE_BY_NAME, method = RequestMethod.POST)
	@ResponseBody
	public boolean removeUsedIngredientByName(
			@RequestBody UsedIngredient usedIngredient) {
		recipeManager.deleteUsedIngredient(usedIngredient);
		return true;
	}

	@RequestMapping(value = Endpoints.RECIPE_ADD_INGREDIENT, method = RequestMethod.POST)
	@ResponseBody
	public boolean addUsedIngredientByName(@RequestBody String param)
			throws JsonProcessingException, IOException {
		ObjectMapper mapper = new ObjectMapper();
		JsonNode node = mapper.readTree(param);
		UsedIngredient usedIngredient = new UsedIngredient();
		usedIngredient.setAmount(mapper.convertValue(node.get("webAmount"),
				Double.class));
		usedIngredient.setUnit(mapper.convertValue(node.get("webUnit"),
				Unit.class));
		usedIngredient.setRecipe(mapper.convertValue(node.get("webRecipe"),
				Recipe.class));
		usedIngredient.setIngredient(mapper.convertValue(
				node.get("webIngredient"), Ingredient.class));

		recipeManager.addIngredient(usedIngredient);
		return true;
	}
}
