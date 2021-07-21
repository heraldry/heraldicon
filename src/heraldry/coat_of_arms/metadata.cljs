(ns heraldry.coat-of-arms.metadata
  (:require [heraldry.license :as license]
            [heraldry.options :as options]))

(defn actual-attribution [title creator creator-url url
                          {:keys [license license-version nature
                                  source-name source-link
                                  source-license source-license-version
                                  source-creator-name source-creator-link]}]
  (let [license-url (license/url license license-version)]
    [:metadata {:xmlns:rdf "http://www.w3.org/1999/02/22-rdf-syntax-ns#"
                :xmlns:cc "http://creativecommons.org/ns#"
                :xmlns:dc "http://purl.org/dc/elements/1.1/"}
     [:rdf:RDF
      [:rdf:Description {:dc:title title
                         :dc:creator creator}]
      [:cc:Work {:rdf:about url}
       [:cc:license {:rdf:resource license-url}]
       [:cc:attributionName (str title " by " creator)]
       [:cc:attributionURL {:rdf:resource creator-url}]]
      [:cc:License {:rdf:about license-url}
       (case license
         :cc-attribution [:<>
                          [:cc:permits {:rdf:resource "http://creativecommons.org/ns#Reproduction"}]
                          [:cc:permits {:rdf:resource "http://creativecommons.org/ns#DerivativeWorks"}]
                          [:cc:permits {:rdf:resource "http://creativecommons.org/ns#Distribution"}]
                          [:cc:requires {:rdf:resource "http://creativecommons.org/ns#Notice"}]
                          [:cc:requires {:rdf:resource "http://creativecommons.org/ns#Attribution"}]]
         :cc-attribution-share-alike [:<>
                                      [:cc:permits {:rdf:resource "http://creativecommons.org/ns#Reproduction"}]
                                      [:cc:permits {:rdf:resource "http://creativecommons.org/ns#DerivativeWorks"}]
                                      [:cc:permits {:rdf:resource "http://creativecommons.org/ns#Distribution"}]
                                      [:cc:requires {:rdf:resource "http://creativecommons.org/ns#Notice"}]
                                      [:cc:requires {:rdf:resource "http://creativecommons.org/ns#Attribution"}]
                                      [:cc:requires {:rdf:resource "http://creativecommons.org/ns#ShareAlike"}]]
         :cc-attribution-non-commercial-share-alike [:<>
                                                     [:cc:permits {:rdf:resource "http://creativecommons.org/ns#Reproduction"}]
                                                     [:cc:permits {:rdf:resource "http://creativecommons.org/ns#DerivativeWorks"}]
                                                     [:cc:permits {:rdf:resource "http://creativecommons.org/ns#Distribution"}]
                                                     [:cc:requires {:rdf:resource "http://creativecommons.org/ns#Notice"}]
                                                     [:cc:requires {:rdf:resource "http://creativecommons.org/ns#Attribution"}]
                                                     [:cc:requires {:rdf:resource "http://creativecommons.org/ns#ShareAlike"}]
                                                     [:cc:prohibits {:rdf:resource "http://creativecommons.org/ns#CommercialUse"}]]
         :publjc-domain [:<>
                         [:cc:permits {:rdf:resource "http://creativecommons.org/ns#Reproduction"}]
                         [:cc:permits {:rdf:resource "http://creativecommons.org/ns#DerivativeWorks"}]
                         [:cc:permits {:rdf:resource "http://creativecommons.org/ns#Distribution"}]]
         nil)]]
     (when (= nature :derivative)
       (let [source-license-url (license/url source-license source-license-version)]
         [:rdf:RDF {:rdf:about source-link}
          [:rdf:Description {:dc:title source-name
                             :dc:creator source-creator-name}]
          [:cc:Work {:rdf:about source-link}
           [:cc:license {:rdf:resource source-license-url}]
           [:cc:attributionName source-creator-name]
           [:cc:attributionURL {:rdf:resource source-creator-link}]]]))]))

(defn attribution [path attribution-type context]
  (let [username (options/raw-value (conj path :username) context)
        creator-url (license/full-url-for-username username)
        url (case attribution-type
              :arms (license/full-url-for-arms path context)
              :charge (license/full-url-for-charge path context))]
    [actual-attribution
     (options/raw-value (conj path :name) context)
     username
     creator-url
     url
     (options/raw-value (conj path :attribution) context)]))
