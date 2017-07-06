/*
 * Copyright (c) 2017. Universidad Politecnica de Madrid
 *
 * @author Badenes Olmedo, Carlos <cbadenes@fi.upm.es>
 *
 */

package org.librairy.eval.evaluations;

import com.fasterxml.jackson.databind.ObjectMapper;
import es.cbadenes.lab.test.IntegrationTest;
import org.junit.Test;
import org.junit.experimental.categories.Category;
import org.librairy.eval.algorithms.*;
import org.librairy.eval.model.Corpora;
import org.librairy.eval.model.DirichletDistribution;
import org.librairy.eval.model.Result;
import org.librairy.eval.model.Similarity;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.util.StringUtils;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * @author Badenes Olmedo, Carlos <cbadenes@fi.upm.es>
 */
@Category(IntegrationTest.class)
public class AlgorithmsEvaluation extends AbstractEvaluation {

    private static final Logger LOG = LoggerFactory.getLogger(AlgorithmsEvaluation.class);

    @Test
    public void evaluate() throws IOException {

        Map<String,Algorithm> algorithms = new HashMap<>();
        algorithms.put("gradient",      new GradientAlgorithm(0.99));
        algorithms.put("entropy-1",      new EntropyAlgorithm(1));
        algorithms.put("entropy-2",      new EntropyAlgorithm(2));
        algorithms.put("entropy-3",      new EntropyAlgorithm(3));
        algorithms.put("hentropy-100",   new HierarchicalEntropyAlgorithm(100));
        algorithms.put("hentropy-200",   new HierarchicalEntropyAlgorithm(200));
        algorithms.put("hentropy-300",   new HierarchicalEntropyAlgorithm(300));
        algorithms.put("kmeans-20",      new KMeansAlgorithm(20));
        algorithms.put("kmeans-50",      new KMeansAlgorithm(50));
        algorithms.put("kmeans-100",     new KMeansAlgorithm(100));
        algorithms.put("dbscan-10",     new DBSCANAlgorithm(10));
        algorithms.put("dbscan-50",     new DBSCANAlgorithm(50));
        algorithms.put("dbscan-100",     new DBSCANAlgorithm(100));
        algorithms.put("dbscan-200",     new DBSCANAlgorithm(200));
        algorithms.put("random",        new RandomizeSelectionAlgorithm(44)); // num topics

        Map<String,DataReport> reports = new HashMap<>();
        reports.put("general",          result -> result.toString()+"\n");
        reports.put("time",             result -> result.getTime() + "\t");
        reports.put("efficiency",       result -> result.getEfficiency() + "\t");
        reports.put("fMeasure",         result -> result.getFMeasure() + "\t");
        reports.put("precision",         result -> result.getPrecision() + "\t");
        reports.put("recall",         result -> result.getRecall() + "\t");
        reports.put("pairs",     result -> result.getCalculatedSimilarities() + "\t");
        reports.put("clusters",         result -> result.getClusters() + "\t");
        reports.put("effectiveness",    result -> result.getEffectiveness() + "\t");

        Double minScore             = 0.83;
        List<Integer> sizes = Arrays.asList(new Integer[]{200,300,400,500,600,700,800,900,1000});

        String fileName = new StringBuilder().append("out/res-top").append("-min").append(StringUtils.replace(String.valueOf(minScore),".","_")).toString();


        Map<String,FileWriter> writers = new HashMap<>();
        reports.entrySet().forEach(entry -> {try {writers.put(entry.getKey(), new FileWriter(new File(fileName + "-" + entry.getKey() + ".txt")));} catch (IOException e) {e.printStackTrace();}});

        // header
        writers.entrySet().forEach(entry -> {try {entry.getValue().write("Size\t");} catch (IOException e) {e.printStackTrace();}});
        algorithms.entrySet().stream().sorted((a,b) -> a.getKey().compareTo(b.getKey())).forEach( algorithm -> writers.entrySet().forEach(writer -> {try {writer.getValue().write(algorithm.getKey() + "\t");} catch (IOException e) {e.printStackTrace();}}));


        for(Integer size: sizes){
            writers.entrySet().forEach(entry -> {try {entry.getValue().write("\n" + size + "\t");} catch (IOException e) {e.printStackTrace();}});

            List<DirichletDistribution> corpus = retrieveCorpus(size);
            Integer numTopics = corpus.get(0).getVector().size();

            Map<String, List<Similarity>> goldStandard = createGoldStandard(corpus, minScore);

            algorithms.entrySet().stream().sorted((a,b) -> a.getKey().compareTo(b.getKey())).forEach( algorithm -> {
                Result result = evaluationOf(size, numTopics, minScore,corpus,goldStandard,algorithm.getValue());
                result.setAlgorithm(algorithm.getKey());

                writers.entrySet().forEach(writer -> {try {writer.getValue().write(reports.get(writer.getKey()).get(result));} catch (IOException e) {e.printStackTrace();}});
            });
        }

        writers.entrySet().forEach(writer -> {try {writer.getValue().close();} catch (IOException e) {e.printStackTrace();}});
    }


    private List<DirichletDistribution> retrieveCorpus(Integer maxSize) throws IOException {
        ObjectMapper jsonMapper = new ObjectMapper();
        Corpora corpora = jsonMapper.readValue(new File("src/main/resources/corpora.json"), Corpora.class);
        return corpora.getDocuments().stream().limit(maxSize).collect(Collectors.toList());

    }

}