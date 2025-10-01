package utils;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Random;

public class CaptionGenerator implements Serializable {
    private static final long serialVersionUID = 1L;
    
    private List<String> captions;

    public CaptionGenerator(){
        captions = new ArrayList<>(Arrays.asList(
                "«Ogni ferita è un ricordo, ogni cicatrice una vittoria.»",
                "«Le regole sono solo suggerimenti per gli altri.»",
                "«Il dado è il mio oracolo, e oggi parla chiaro.»",
                "«Non servo gli dei, sono gli dei che servono me.»",
                "«Ogni tiro basso è solo un preludio al critico.»",
                "«La scheda dice eroe, il mondo dice minaccia.»",
                "«Non tiro per colpire. Tiro per uccidere!»",
                "«Il fato ha scelto, e io ho risposto.»",
                "«Ogni passo lascia un segno nella storia.»",
                "«Il silenzio prima della battaglia è la mia preghiera.»",
                "«Porto con me il peso di mille giuramenti.»",
                "«Dove cala l’ombra, la mia lama è già pronta.»",
                "«Il sangue versato è il prezzo del cammino.»",
                "«Cammino tra i vivi, ma il mio destino parla di leggende.»",
                "«Ogni cicatrice è un frammento della mia verità.»",
                "«Il tempo teme chi non conosce paura.»",
                "«La mia volontà è più affilata di qualsiasi lama.»",
                "«Sono figlio del caos, ma servo solo la mia strada.»",
                "«Gli dèi osservano. Io agisco.»",
                "«Non sono nato per seguire, ma per segnare la via.»",
                "«Il mio nome è inciso nel tuono delle battaglie.»",
                "«Ho perso tutto, e proprio per questo non temo nulla.»"));
    }

    public String getRandom() {
        return captions.get(new Random().nextInt(captions.size()));
    }

    public void addCaption(String caption) {
        if (caption != null) {
            caption = caption.trim();
            if (!caption.isEmpty()) {
                if (!caption.startsWith("«")) {
                    caption = "«" + caption + "»";
                }
                captions.add(caption);
            }
        }
    }

    public void setCustomCaptions(String customList) {
        List<String> newCaptions = new ArrayList<>();
        String[] lines = customList.split("\n");

        for (String line : lines) {
            line = line.trim();
            if (line.startsWith("-")) {
                line = line.substring(1).trim();
                if (!line.isEmpty()) {
                    if (!line.startsWith("«")) {
                        line = "«" + line + "»";
                    }
                    newCaptions.add(line);
                }
            }
        }

        if (!newCaptions.isEmpty()) {
            captions = newCaptions;
        }
    }

    public void resetToDefault() {
        captions = new CaptionGenerator().captions;
    }

    public String getAllFormatted() {
        StringBuilder sb = new StringBuilder();
        for (String caption : captions) {
            sb.append("- ").append(caption).append("\n");
        }
        return sb.toString().trim();
    }

    public boolean validateCustomCaptions(String customList) {
        if (customList == null || customList.isBlank()) {
            return false;
        }
    
        String[] lines = customList.split("\n");
        for (String line : lines) {
            line = line.trim();
            if (!line.startsWith("-")) {
                return false;
            }
            String content = line.substring(1).trim();
            if (content.isEmpty()) {
                return false;
            }
        }
        return true;
    }
}