/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package game.pcg;

import game.GameState;
import optimization.EmbeddingComparator;
import orthographicembedding.OrthographicEmbeddingResult;

/**
 *
 * @author josepvalls
 */
public class PuzzleEmbeddingComparator implements EmbeddingComparator{
    PuzzleEmbeddingEvaluator pee;
    public PuzzleEmbeddingComparator(PuzzleEmbeddingEvaluator pee){
        this.pee = pee;
    }
    @Override
    public int compare(OrthographicEmbeddingResult oer1, OrthographicEmbeddingResult oer2) {
        // The optimization process minimizes the evaluation
        return Double.compare(pee.evaluate(oer1), pee.evaluate(oer2));
    }
    
}
