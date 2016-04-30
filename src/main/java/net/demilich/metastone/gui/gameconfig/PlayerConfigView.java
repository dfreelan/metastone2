package net.demilich.metastone.gui.gameconfig;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import javafx.beans.value.ChangeListener;
import javafx.beans.value.ObservableValue;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.control.CheckBox;
import javafx.scene.control.ComboBox;
import javafx.scene.control.Label;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.VBox;
import net.demilich.metastone.game.behaviour.GreedyOptimizeMove;
import net.demilich.metastone.game.behaviour.IBehaviour;
import net.demilich.metastone.game.behaviour.NoAggressionBehaviour;
import net.demilich.metastone.game.behaviour.PlayRandomBehaviour;
import net.demilich.metastone.game.behaviour.PlayAllRandomBehavior;
import net.demilich.metastone.game.behaviour.decicionTreeBheavior.DecisionTreeBehaviour;
import net.demilich.metastone.game.behaviour.experimentalMCTS.ExperimentalMCTS;
import net.demilich.metastone.game.behaviour.heuristic.WeightedHeuristic;
import net.demilich.metastone.game.behaviour.human.HumanBehaviour;
import net.demilich.metastone.game.behaviour.human.HumanDebugBehaviour;
import net.demilich.metastone.game.behaviour.threat.GameStateValueBehaviour;
import net.demilich.metastone.game.cards.Card;
import net.demilich.metastone.game.cards.CardCatalogue;
import net.demilich.metastone.game.cards.HeroCard;
import net.demilich.metastone.game.decks.Deck;
import net.demilich.metastone.game.decks.DeckFactory;
import net.demilich.metastone.game.entities.heroes.HeroClass;
import net.demilich.metastone.game.entities.heroes.MetaHero;
import net.demilich.metastone.gui.common.BehaviourStringConverter;
import net.demilich.metastone.gui.common.DeckStringConverter;
import net.demilich.metastone.gui.common.HeroStringConverter;
import net.demilich.metastone.gui.playmode.config.PlayerConfigType;

public class PlayerConfigView extends VBox {

	@FXML
	protected Label heroNameLabel;

	@FXML
	protected ImageView heroIcon;

	@FXML
	protected ComboBox<IBehaviour> behaviourBox;

	@FXML
	protected ComboBox<HeroCard> heroBox;

	@FXML
	protected ComboBox<Deck> deckBox;

	@FXML
	protected CheckBox hideCardsCheckBox;

	private final PlayerConfig playerConfig = new PlayerConfig();

	private List<Deck> decks = new ArrayList<Deck>();

	private PlayerConfigType selectionHint;

	public PlayerConfigView(PlayerConfigType selectionHint) {
		this.selectionHint = selectionHint;
		FXMLLoader fxmlLoader = new FXMLLoader(getClass().getResource("/fxml/PlayerConfigView.fxml"));
		fxmlLoader.setRoot(this);
		fxmlLoader.setController(this);

		try {
			fxmlLoader.load();
		} catch (IOException exception) {
			throw new RuntimeException(exception);
		}

		heroBox.setConverter(new HeroStringConverter());
		deckBox.setConverter(new DeckStringConverter());
		behaviourBox.setConverter(new BehaviourStringConverter());

		setupHideCardsBox(selectionHint);
		setupHeroes();
		setupBehaviours();
		deckBox.valueProperty().addListener((ChangeListener<Deck>) (observableProperty, oldDeck, newDeck) -> {
			getPlayerConfig().setDeck(newDeck);
		});
	}

	private void filterDecks() {
		HeroClass heroClass = getPlayerConfig().getHeroCard().getHeroClass();
		ObservableList<Deck> deckList = FXCollections.observableArrayList();

		if (heroClass == HeroClass.DECK_COLLECTION) {
			for (Deck deck : decks) {
				if (deck.getHeroClass() != HeroClass.DECK_COLLECTION) {
					continue;
				}
				deckList.add(deck);
			}
		} else {
			Deck randomDeck = DeckFactory.getRandomDeck(heroClass);
			deckList.add(randomDeck);
			for (Deck deck : decks) {
				if (deck.getHeroClass() == HeroClass.DECK_COLLECTION) {
					continue;
				}
				if (deck.getHeroClass() == heroClass || deck.getHeroClass() == HeroClass.ANY) {
					deckList.add(deck);
				}
			}
		}

		deckBox.setItems(deckList);
		deckBox.getSelectionModel().selectFirst();
	}

	public PlayerConfig getPlayerConfig() {
		return playerConfig;
	}

	public void injectDecks(List<Deck> decks) {
		this.decks = decks;
		heroBox.getSelectionModel().selectFirst();
		behaviourBox.getSelectionModel().selectFirst();
	}

	private void onBehaviourChanged(ObservableValue<? extends IBehaviour> ov, IBehaviour oldBehaviour, IBehaviour newBehaviour) {
		getPlayerConfig().setBehaviour(newBehaviour);
		boolean humanBehaviourSelected = newBehaviour instanceof HumanBehaviour;
		hideCardsCheckBox.setDisable(humanBehaviourSelected);
		if (humanBehaviourSelected) {
			hideCardsCheckBox.setSelected(false);
		}
	}

	private void onHideCardBoxChanged(ObservableValue<? extends Boolean> ov, Boolean oldValue, Boolean newValue) {
		playerConfig.setHideCards(newValue);
	}

	private void selectHero(HeroCard HeroCard) {
		Image heroPortrait = HeroCard.getImage();
		heroIcon.setImage(heroPortrait);
		heroNameLabel.setText(HeroCard.getName());
		getPlayerConfig().setHeroCard(HeroCard);
		filterDecks();
	}

	public void setupBehaviours() {
		ObservableList<IBehaviour> behaviourList = FXCollections.observableArrayList();
		if (selectionHint == PlayerConfigType.HUMAN || selectionHint == PlayerConfigType.SANDBOX) {
			behaviourList.add(new HumanDebugBehaviour());
		}

		behaviourList.add(new GameStateValueBehaviour());

		if (selectionHint == PlayerConfigType.OPPONENT) {
			behaviourList.add(new HumanDebugBehaviour());
		}

		behaviourList.add(new PlayRandomBehaviour());

		behaviourList.add(new GreedyOptimizeMove(new WeightedHeuristic()));
		behaviourList.add(new NoAggressionBehaviour());
                //10000 and 60 currently candidate best
                //best of 2, 1.2 explore , 10000 rollouts, 60 trees, 5 features, both probmax
                //41:9
                //34:16
                //RESET TREE: 
                //35:15
                //41:9
                //Default play all rando,otherwise same settings as above... notice lack of variance
                //36:14
                //37:13
                //36:14
                //True best of two, not throwing out <.5
                //32:18
                //10000 and 30:
                //39:11 (took 40 minutes!)
                //36:14 took 43 minutes!
                ExperimentalMCTS singleton = new ExperimentalMCTS(40000,15,1.414,false);
                singleton.setName("10000 and 60 trees, not clairvoyant");
		behaviourList.add(singleton);
                
                ExperimentalMCTS quadTree = new ExperimentalMCTS(10000,30,1.2,false);
                quadTree.setName("10000 and 30 trees");
		behaviourList.add(quadTree);
                
                behaviourList.add(new DecisionTreeBehaviour());
                
                behaviourList.add(new PlayAllRandomBehavior());
		behaviourBox.setItems(behaviourList);
		behaviourBox.valueProperty().addListener(this::onBehaviourChanged);
	}

	public void setupHeroes() {
		ObservableList<HeroCard> heroList = FXCollections.observableArrayList();
		for (Card card : CardCatalogue.getHeroes()) {
			heroList.add((HeroCard) card);
		}

		heroList.add(new MetaHero());

		heroBox.setItems(heroList);
		heroBox.valueProperty().addListener((ChangeListener<HeroCard>) (observableValue, oldHero, newHero) -> {
			selectHero(newHero);
		});
	}

	private void setupHideCardsBox(PlayerConfigType configType) {
		hideCardsCheckBox.selectedProperty().addListener(this::onHideCardBoxChanged);
		hideCardsCheckBox.setSelected(selectionHint == PlayerConfigType.OPPONENT);
		if (configType == PlayerConfigType.SIMULATION || configType == PlayerConfigType.SANDBOX) {
			hideCardsCheckBox.setVisible(false);
		}
	}
}

