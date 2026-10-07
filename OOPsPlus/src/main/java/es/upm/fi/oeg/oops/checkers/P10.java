/*
 * SPDX-FileCopyrightText: 2014 María Poveda Villalón <mpovedavillalon@gmail.com>
 * SPDX-FileCopyrightText: 2025 Robin Vobruba <hoijui.quaero@gmail.com>
 *
 * SPDX-License-Identifier: Apache-2.0
 */

package es.upm.fi.oeg.oops.checkers;

import static es.upm.fi.oeg.oops.Constants.LLM_IP;
import static es.upm.fi.oeg.oops.Constants.LLM_MODEL;

import dev.langchain4j.model.ollama.OllamaChatModel;
import es.upm.fi.oeg.oops.Arity;
import es.upm.fi.oeg.oops.Checker;
import es.upm.fi.oeg.oops.CheckerInfo;
import es.upm.fi.oeg.oops.CheckingContext;
import es.upm.fi.oeg.oops.Importance;
import es.upm.fi.oeg.oops.PitfallCategoryId;
import es.upm.fi.oeg.oops.PitfallId;
import es.upm.fi.oeg.oops.PitfallInfo;
import es.upm.fi.oeg.oops.PitfallInfo.AccompPer;
import es.upm.fi.oeg.oops.RuleScope;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Set;
import org.apache.jena.ontology.ConversionException;
import org.apache.jena.ontology.OntClass;
import org.apache.jena.ontology.OntModel;
import org.apache.jena.util.iterator.ExtendedIterator;
import org.kohsuke.MetaInfServices;
import org.semanticweb.owlapi.model.AxiomType;
import org.semanticweb.owlapi.model.OWLAxiom;
import org.semanticweb.owlapi.model.OWLOntology;

/**
 * Esta pitfall no se da en un elemento concreto, se da, o no se da, en la ontolog�a si no hay disjoints.
 */
@MetaInfServices(Checker.class)
public class P10 implements Checker {

    private static final PitfallInfo PITFALL_INFO = new PitfallInfo(new PitfallId(10, null),
            Set.of(new PitfallCategoryId('N', 4), new PitfallCategoryId('S', 5)), Importance.IMPORTANT,
            "Missing disjoint-ness",
            "The ontology lacks disjoint axioms between classes "
                    + "or between properties that should be defined as disjoint. "
                    + "This pitfall is related to the guidelines provided in [6], [2] and [7].",
            RuleScope.CLASS, Arity.ONE, "There are no owl:disjointWith axioms defined.", AccompPer.TYPE);

    public static final CheckerInfo INFO = new CheckerInfo(PITFALL_INFO);

    @Override
    public CheckerInfo getInfo() {
        return INFO;
    }
    public static String askLLM(String nombre, List<OntClass> subClassesList) {
        // Configuramos el modelo local
        OllamaChatModel model = OllamaChatModel.builder().baseUrl(LLM_IP).modelName(LLM_MODEL).build();
        String classesName = "";
        for (int i = 0; i < subClassesList.size(); i++) {
            classesName = classesName + ", " + subClassesList.get(i).getLocalName();
        }
        System.out.println("P10" + classesName);
        // Hacemos la petición
        String respuesta = model.generate("In the context of " + nombre
                + "Determine if the following concepts are DISJOINT"
                + " (They are mutually exclusive concepts that do not share any common ground, so it is impossible for an element to belong to both at the same time) "
                + "or NOT. Return only yes or no." + "The concepts are:" + classesName);

        return respuesta;

    }

    @Override
    public void check(final CheckingContext context) {

        final OntModel model = context.getModel();
        final OWLOntology onto = context.getModelOwl();

        // if there are less then 2 classes,
        // there is no possibility for a lack of disjointness.
        if (model.listNamedClasses().toList().size() < 2) {
            return;
        }

        ExtendedIterator<OntClass> classes = model.listNamedClasses();
        while (classes.hasNext()) {
            final OntClass ontoClass = classes.next();
            final ExtendedIterator<OntClass> disjoints = ontoClass.listDisjointWith();

            do {
                try {
                    if (disjoints.hasNext()) {
                        // if there is even a single disjoint,
                        // there is no pitfall
                        return;
                    }

                } catch (final ConversionException exc) {
                    final String classUri = exc.getMessage().split(" ")[3];
                    final OntClass cAux = context.getModel().createClass(classUri);
                    context.addClassWarning(INFO, cAux,
                            "The attached resources do not have `rdf:type owl:Class` or equivalent");
                    classes = model.listNamedClasses();
                    continue;
                }
            } while (false);//??
        }

        // ahora probamos con owl2 OWL API
        for (final OWLAxiom owlAxiom : onto.getAxioms()) {
            // final AxiomType axiomType = owlAxiom.getAxiomType();
            // Only whether the axiom is a SubPropertyChainOf axiom
            final boolean isDisjointClasses = owlAxiom.isOfType(AxiomType.DISJOINT_CLASSES);
            final boolean isDisjointUnion = owlAxiom.isOfType(AxiomType.DISJOINT_UNION);

            if (isDisjointClasses || isDisjointUnion) {
                return;
            }
        }
        //tecnicamente aqui solo se llega si no se ha detectacion disjoint en ningun punto
        List<OntClass> classesList = model.listNamedClasses().toList();
        List<OntClass> subClases = new ArrayList<OntClass>(); 
        // System.out.println("TAMAÑO LISTA DE CLASES " + classesList.size());
        //se buscan las clases raiz para buscar sus subclases
        for (int i = 0; i < classesList.size(); i++) {
            if (classesList.get(i).hasSubClass()) {
                if (classesList.get(i).listSubClasses().toList().size() > 1) {
                    subClases.add(classesList.get(i));
                    // System.out.println("Creando lista de clases " + classesList.get(i).toString());
                }
            }
        }

        for (int j = 0; j < subClases.size(); j++) {
            //AQUI SE LLAMA AL LLM PARA PREGUNTARLE SI SON DISJUNTOS
            List<OntClass> subClassesList = subClases.get(j).listSubClasses().toList();
            String nombre = subClases.get(j).getLocalName();
            System.out.println(nombre);
            String respuesta = askLLM(nombre, subClassesList);
            respuesta = respuesta.replaceAll("\\W", "").replaceAll("-", "").replaceAll("\\.", "").replaceAll("_", "")
                    .toLowerCase();
            System.out.println("P10 Respuesta " + respuesta);
            if (respuesta.equals("yes")) {            
                for (int k = 0; k < subClassesList.size(); k++) {
                    System.out.println(subClassesList.get(k));
                    context.addResult(PITFALL_INFO, subClassesList.get(k));

                }
            }
        }

        //ESTE ES EL FINAL

        // context.addResult(PITFALL_INFO, Collections.emptySet());
    }
}
