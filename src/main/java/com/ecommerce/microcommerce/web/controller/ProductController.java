package com.ecommerce.microcommerce.web.controller;

import com.ecommerce.microcommerce.dao.ProductDao;
import com.ecommerce.microcommerce.model.Product;
import com.ecommerce.microcommerce.web.exceptions.AucunProduitEnMagasinException;
import com.ecommerce.microcommerce.web.exceptions.ProduitGratuitException;
import com.ecommerce.microcommerce.web.exceptions.ProduitIntrouvableException;
import com.fasterxml.jackson.databind.ser.FilterProvider;
import com.fasterxml.jackson.databind.ser.impl.SimpleBeanPropertyFilter;
import com.fasterxml.jackson.databind.ser.impl.SimpleFilterProvider;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.json.MappingJacksonValue;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.support.ServletUriComponentsBuilder;

import javax.validation.Valid;
import java.net.URI;
import java.util.HashMap;
import java.util.List;
import java.util.Map;


@Api(description = "API pour es opérations CRUD sur les produits.")
@RestController
public class ProductController {

    private final ProductDao productDao;
    private final int ZERO_EURO = 0;

    @Autowired
    public ProductController(ProductDao productDao) {
        this.productDao = productDao;
    }


    //Récupérer la liste des produits

    @RequestMapping(value = "/Produits", method = RequestMethod.GET)
    public MappingJacksonValue listeProduits() {

//        Iterable<Product> produits = productDao.findAll();

        //Partie 2 - Tri par ordre alphabétique
        Iterable<Product> produits = productDao.findAllByOrderByNomAsc();


        SimpleBeanPropertyFilter monFiltre = SimpleBeanPropertyFilter.serializeAllExcept("prixAchat");

        FilterProvider listDeNosFiltres = new SimpleFilterProvider().addFilter("monFiltreDynamique", monFiltre);

        MappingJacksonValue produitsFiltres = new MappingJacksonValue(produits);

        produitsFiltres.setFilters(listDeNosFiltres);

        return produitsFiltres;
    }


    //Récupérer un produit par son Id
    @ApiOperation(value = "Récupère un produit grâce à son ID à condition que celui-ci soit en stock!")
    @GetMapping(value = "/Produits/{id}")
    public Product afficherUnProduit(@PathVariable int id)  {

        Product produit = productDao.findById(id);

        if (produit == null)
            throw new ProduitIntrouvableException("Le produit avec l'id " + id + " est INTROUVABLE. Écran Bleu si je pouvais.");

        return produit;
    }


    //ajouter un produit
    @PostMapping(value = "/Produits")

    public ResponseEntity<Void> ajouterProduit(@Valid @RequestBody Product product) throws ProduitGratuitException {

        //Partie 3 - Validation du prix de vente
        if(product.getPrix() == ZERO_EURO) throw new ProduitGratuitException("Produit Gratuit ! Nous devons tourner l'économie");
        Product productAdded = productDao.save(product);

        if (productAdded == null)
            return ResponseEntity.noContent().build();

        URI location = ServletUriComponentsBuilder
                .fromCurrentRequest()
                .path("/{id}")
                .buildAndExpand(productAdded.getId())
                .toUri();

        return ResponseEntity.created(location).build();
    }

    @DeleteMapping(value = "/Produits/{id}")
    public void supprimerProduit(@PathVariable int id) {

        productDao.delete(id);
    }

    @PutMapping(value = "/Produits")
    public void updateProduit(@RequestBody Product product) {

        productDao.save(product);
    }


    //Partie 1 - Affichage de la marge
    @ApiOperation(value = "Calcule des marges produits")
    @GetMapping(value = "/AdminProduits")
    public MappingJacksonValue calculerMargeProduit() {
        int margePrix ;
        Iterable<Product> produits = productDao.findAll();

        Map<Object,Integer> listMargeProduits = new HashMap<>();

        if(produits == null )throw new AucunProduitEnMagasinException("Aucuns produit en magasin");

        for (Product produit : produits) {

            margePrix =  produit.getPrix() - produit.getPrixAchat();

            listMargeProduits.put(produit,margePrix);
        }

//        MappingJacksonValue listeProduitsMarges = new MappingJacksonValue(listMargeProduits);

        return  new MappingJacksonValue(listMargeProduits);

    }



    //Pour les tests
    @GetMapping(value = "test/produits/{prix}")
    public List<Product> testeDeRequetes(@PathVariable int prix) {

        return productDao.chercherUnProduitCher(400);
    }


}
