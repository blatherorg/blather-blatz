package com.blather;

import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.beans.factory.annotation.Autowired;
import java.util.List;

@RestController
public class BlatController {

    @Autowired
    private BlatService blatService;

    @RequestMapping("/blatz")
    public List<Blat> getBlats() {
        List <Blat> blats = null;

        blats = blatService.getBlats();
        return blats;
    }

    @RequestMapping(value = "/blatz/{handle}", method = RequestMethod.GET)
    public List<Blat> getBlatsByHandle(@PathVariable String handle) {
        List <Blat> blats = null;

        blats = blatService.getBlatsByHandle(handle);
        return blats;
    }

    @RequestMapping(value = "/blatz", method = RequestMethod.POST)
    public void postBlat(@RequestBody Blat blat) {
        blatService.loadBlat(blat);
    }
}
