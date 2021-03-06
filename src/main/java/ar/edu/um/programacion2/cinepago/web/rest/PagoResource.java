package ar.edu.um.programacion2.cinepago.web.rest;

import java.math.BigDecimal;
import java.net.URI;
import java.net.URISyntaxException;
import java.time.ZonedDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import javax.validation.Valid;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpHeaders;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.codahale.metrics.annotation.Timed;

import ar.edu.um.programacion2.cinepago.domain.Pago;
import ar.edu.um.programacion2.cinepago.domain.Tarjeta;
import ar.edu.um.programacion2.cinepago.repository.PagoRepository;
import ar.edu.um.programacion2.cinepago.repository.TarjetaRepository;
import ar.edu.um.programacion2.cinepago.web.rest.errors.BadRequestAlertException;
import ar.edu.um.programacion2.cinepago.web.rest.util.HeaderUtil;
import ar.edu.um.programacion2.cinepago.web.rest.util.PaginationUtil;
import io.github.jhipster.web.util.ResponseUtil;

/**
 * REST controller for managing Pago.
 */
@RestController
@RequestMapping("/api")
public class PagoResource {

    private final Logger log = LoggerFactory.getLogger(PagoResource.class);

    private static final String ENTITY_NAME = "pago";
    
    private final PagoRepository pagoRepository;
    
    @Autowired
    private TarjetaRepository tarjetaRepository;

    public PagoResource(PagoRepository pagoRepository) {
        this.pagoRepository = pagoRepository;
    }

    /**
     * POST  /pagos : Create a new pago.
     *
     * @param pago the pago to create
     * @return the ResponseEntity with status 201 (Created) and with body the new pago, or with status 400 (Bad Request) if the pago has already an ID
     * @throws URISyntaxException if the Location URI syntax is incorrect
     */
    @PostMapping("/pagos")
    @Timed
    public ResponseEntity<Pago> createPago(@Valid @RequestBody Pago pago) throws URISyntaxException {
        log.debug("REST request to save Pago : {}", pago);
        if (pago.getId() != null) {
            throw new BadRequestAlertException("A new pago cannot already have an ID", ENTITY_NAME, "idexists");
        }
        
        Tarjeta tarjeta = pago.getTarjeta();
        
        if(tarjeta.getSaldo().compareTo(pago.getImporte()) == -1 ) {
        	throw new UnsupportedOperationException("Saldo Insuficiente");
        }
        
        tarjeta.setSaldo(tarjeta.getSaldo().subtract(pago.getImporte()));
        tarjeta.setUpdated(ZonedDateTime.now());
        tarjetaRepository.save(tarjeta);
        
        // Agrego el UUID dinamicamente	 
        pago.setPagoUuid(UUID.randomUUID().toString());
        
        //cargo la fecha actual y de actualizacion
        pago.setCreated(ZonedDateTime.now());
        pago.setUpdated(ZonedDateTime.now());
        
        Pago result = pagoRepository.save(pago);
        
        return ResponseEntity.created(new URI("/api/pagos/" + result.getId()))
            .headers(HeaderUtil.createEntityCreationAlert(ENTITY_NAME, result.getId().toString()))
            .body(result);
    }

    /**
     * POST  /pagos/{num_tarjeta}/{importe} : Crea un nuevo pago en una tarjeta de credito con un saldo en particular.
     *
     * @param pago the pago to create
     * @return the String with status 200 and with body the new pago, or with status 501 if the pago has already an ID
     * @throws URISyntaxException if the Location URI syntax is incorrect
     */
    @PostMapping("/pagos/{num_tarjeta}/{importe}")
    @Timed
    public String createPagoTarjeta(@PathVariable String num_tarjeta,@PathVariable BigDecimal importe) throws URISyntaxException {
        log.debug("REST request to save Pago : {}");

        if( tarjetaRepository.findByNumero(num_tarjeta) == null ) {
            throw new BadRequestAlertException("No existe tarjeta", ENTITY_NAME, "num_tarjeta");
        }
        
        Tarjeta tarjeta = tarjetaRepository.findByNumero(num_tarjeta);
        
        if(tarjeta.getSaldo().compareTo(importe) == -1)  {
            throw new BadRequestAlertException("Saldo Insuficiente", ENTITY_NAME, "saldo");
        }
        
        Pago pago=new Pago();
        pago.setImporte(importe);
        pago.setTarjeta(tarjeta);
        pago.setCreated(ZonedDateTime.now());
        pago.setUpdated(ZonedDateTime.now());
        pago.setPagoUuid(UUID.randomUUID().toString());
        pagoRepository.save(pago);
        
        tarjeta.setSaldo(tarjeta.getSaldo().subtract(pago.getImporte()));

        tarjetaRepository.save(tarjeta);
        return pago.getPagoUuid();
 
    }
    
    /**
     * PUT  /pagos : Updates an existing pago.
     *
     * @param pago the pago to update
     * @return the ResponseEntity with status 200 (OK) and with body the updated pago,
     * or with status 400 (Bad Request) if the pago is not valid,
     * or with status 500 (Internal Server Error) if the pago couldn't be updated
     * @throws URISyntaxException if the Location URI syntax is incorrect
     */
    @PutMapping("/pagos")
    @Timed
    public ResponseEntity<Pago> updatePago(@Valid @RequestBody Pago pago) throws URISyntaxException {
        log.debug("REST request to update Pago : {}", pago);
        if (pago.getId() == null) {
            throw new BadRequestAlertException("Invalid id", ENTITY_NAME, "idnull");
        }
        
        // Actualizo las fechas de actualizacion
        pago.setUpdated(ZonedDateTime.now());
        Pago result = pagoRepository.save(pago);
        return ResponseEntity.ok()
            .headers(HeaderUtil.createEntityUpdateAlert(ENTITY_NAME, pago.getId().toString()))
            .body(result);
    }

    /**
     * GET  /pagos : get all the pagos.
     *
     * @param pageable the pagination information
     * @return the ResponseEntity with status 200 (OK) and the list of pagos in body
     */
    @GetMapping("/pagos")
    @Timed
    public ResponseEntity<List<Pago>> getAllPagos(Pageable pageable) {
        log.debug("REST request to get a page of Pagos");
        Page<Pago> page = pagoRepository.findAll(pageable);
        HttpHeaders headers = PaginationUtil.generatePaginationHttpHeaders(page, "/api/pagos");
        return ResponseEntity.ok().headers(headers).body(page.getContent());
    }

    /**
     * GET  /pagos/:id : get the "id" pago.
     *
     * @param id the id of the pago to retrieve
     * @return the ResponseEntity with status 200 (OK) and with body the pago, or with status 404 (Not Found)
     */
    @GetMapping("/pagos/{id}")
    @Timed
    public ResponseEntity<Pago> getPago(@PathVariable Long id) {
        log.debug("REST request to get Pago : {}", id);
        Optional<Pago> pago = pagoRepository.findById(id);
        return ResponseUtil.wrapOrNotFound(pago);
    }

    /**
     * DELETE  /pagos/:id : delete the "id" pago.
     *
     * @param id the id of the pago to delete
     * @return the ResponseEntity with status 200 (OK)
     */
    @DeleteMapping("/pagos/{id}")
    @Timed
    public ResponseEntity<Void> deletePago(@PathVariable Long id) {
        log.debug("REST request to delete Pago : {}", id);

        pagoRepository.deleteById(id);
        return ResponseEntity.ok().headers(HeaderUtil.createEntityDeletionAlert(ENTITY_NAME, id.toString())).build();
    }
}
