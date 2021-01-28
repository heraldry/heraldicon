(ns heraldry.coat-of-arms.metadata)

(defn attribution [title creator url {:keys [license nature
                                             source-name source-link
                                             source-creator-name source-creator-link]}]
  (let [license-url (cond
                      (= license :cc-attribution)             "https://creativecommons.org/licenses/by/4.0"
                      (= license :cc-attribution-share-alike) "https://creativecommons.org/licenses/by-sa/4.0"
                      (= license :public-domain)              "https://creativecommons.org/publicdomain/mark/1.0/"
                      :else                                   "private")]
    [:metadata {:xmlns:rdf "http://www.w3.org/1999/02/22-rdf-syntax-ns#"
                :xmlns:cc  "http://creativecommons.org/ns#"
                :xmlns:dc  "http://purl.org/dc/elements/1.1/"}
     [:rdf:RDF
      [:rdf:Description {:dc:title   title
                         :dc:creator creator}]
      [:cc:Work {:rdf:about ""}
       [:cc:license {:rdf:resource license-url}]
       [:cc:attributionName (str title " by " creator)]
       [:cc:attributionURL {:rdf:resource url}]]
      [:cc:License {:rdf:about license-url}
       (cond
         (= license :cc-attribution)             [:<>
                                                  [:cc:permits {:rdf:resource "http://creativecommons.org/ns#Reproduction"}]
                                                  [:cc:permits {:rdf:resource "http://creativecommons.org/ns#DerivativeWorks"}]
                                                  [:cc:permits {:rdf:resource "http://creativecommons.org/ns#Distribution"}]
                                                  [:cc:requires {:rdf:resource "http://creativecommons.org/ns#Notice"}]
                                                  [:cc:requires {:rdf:resource "http://creativecommons.org/ns#Attribution"}]]
         (= license :cc-attribution-share-alike) [:<>
                                                  [:cc:permits {:rdf:resource "http://creativecommons.org/ns#Reproduction"}]
                                                  [:cc:permits {:rdf:resource "http://creativecommons.org/ns#DerivativeWorks"}]
                                                  [:cc:permits {:rdf:resource "http://creativecommons.org/ns#Distribution"}]
                                                  [:cc:requires {:rdf:resource "http://creativecommons.org/ns#Notice"}]
                                                  [:cc:requires {:rdf:resource "http://creativecommons.org/ns#Attribution"}]
                                                  [:cc:requires {:rdf:resource "http://creativecommons.org/ns#ShareAlike"}]]
         (= license :public-domain)              [:<>
                                                  [:cc:permits {:rdf:resource "http://creativecommons.org/ns#Reproduction"}]
                                                  [:cc:permits {:rdf:resource "http://creativecommons.org/ns#DerivativeWorks"}]
                                                  [:cc:permits {:rdf:resource "http://creativecommons.org/ns#Distribution"}]])]]
     (when (= nature :derivative)
       (let [source-license-url (cond
                                  (= license :cc-attribution)             "https://creativecommons.org/licenses/by/4.0"
                                  (= license :cc-attribution-share-alike) "https://creativecommons.org/licenses/by-sa/4.0"
                                  (= license :public-domain)              "https://creativecommons.org/publicdomain/mark/1.0/"
                                  :else                                   "private")]
         [:rdf:RDF {:rdf:about source-link}
          [:rdf:Description {:dc:title   source-name
                             :dc:creator source-creator-name}]
          [:cc:Work {:rdf:about source-link}
           [:cc:license {:rdf:resource source-license-url}]
           [:cc:attributionName source-creator-name]
           [:cc:attributionURL {:rdf:resource source-creator-link}]]]))]))
