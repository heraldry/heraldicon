(ns heraldicon.heraldry.facets-test
  (:require
   [cljs.test :refer-macros [deftest is testing]]
   [heraldicon.heraldry.facets :as facets]))

(defn- facets [arms-data]
  (set (facets/facets-of arms-data)))

(deftest plain-field
  (let [tokens (facets {:achievement
                        {:coat-of-arms
                         {:field {:type :heraldry.field.type/plain
                                  :tincture :azure}}}})]
    (is (contains? tokens "tincture:azure"))
    (is (contains? tokens "partition:plain"))
    (is (contains? tokens "field:plain"))
    (is (contains? tokens "field:azure"))
    (is (contains? tokens "main-field:plain"))
    (is (contains? tokens "main-field:azure"))))

(deftest per-pale-with-two-subfields
  (let [tokens (facets {:achievement
                        {:coat-of-arms
                         {:field {:type :heraldry.field.type/per-pale
                                  :fields [{:type :heraldry.field.type/plain
                                            :tincture :or}
                                           {:type :heraldry.field.type/chequy
                                            :tincture :azure}]}}}})]
    (is (contains? tokens "partition:per-pale"))
    (is (contains? tokens "field:per-pale"))
    (is (contains? tokens "main-field:per-pale"))
    (is (contains? tokens "tincture:or"))
    (is (contains? tokens "tincture:azure"))
    (is (contains? tokens "partition:chequy"))
    (is (contains? tokens "field:chequy"))
    (is (contains? tokens "field:or"))
    (is (contains? tokens "field:azure"))
    (testing "main-field is only the top level"
      (is (not (contains? tokens "main-field:chequy")))
      (is (not (contains? tokens "main-field:or"))))))

(deftest ordinary-with-cottise-tinctures
  (let [tokens (facets {:achievement
                        {:coat-of-arms
                         {:field {:type :heraldry.field.type/plain
                                  :tincture :argent
                                  :components
                                  [{:type :heraldry.ordinary.type/bend
                                    :field {:type :heraldry.field.type/plain
                                            :tincture :gules}
                                    :cottise-1 {:field {:type :heraldry.field.type/plain
                                                        :tincture :sable}}
                                    :cottise-2 {:field {:type :heraldry.field.type/plain
                                                        :tincture :vert}}}]}}}})]
    (is (contains? tokens "ordinary:bend"))
    (is (contains? tokens "tincture:argent"))
    (is (contains? tokens "tincture:gules"))
    (testing "cottise tinctures are included"
      (is (contains? tokens "tincture:sable"))
      (is (contains? tokens "tincture:vert")))))

(deftest charge-and-attitude
  (let [tokens (facets {:achievement
                        {:coat-of-arms
                         {:field {:type :heraldry.field.type/plain
                                  :tincture :or
                                  :components
                                  [{:type :heraldry.charge.type/lion
                                    :attitude :rampant
                                    :tincture :sable
                                    :variant {:id "charge:abc" :version 1}}]}}}})]
    (is (contains? tokens "charge:lion"))
    (is (contains? tokens "attitude:rampant"))
    (is (contains? tokens "tincture:sable"))
    (testing "no field:<tincture> on a charge node (it has no field type)"
      (is (not (contains? tokens "field:sable"))))))

(deftest ornaments-and-helms
  (testing "ornaments/helms entries live under :elements of their container"
    (let [tokens (facets {:achievement
                          {:coat-of-arms {:field {:type :heraldry.field.type/plain
                                                  :tincture :argent}}
                           :ornaments {:type :heraldry/ornaments
                                       :elements [{:type :heraldry.motto.type/motto}
                                                  {:type :heraldry.motto.type/slogan}
                                                  {:type :heraldry.charge.type/crown}]}
                           :helms {:type :heraldry/helms
                                   :elements [{:type :heraldry/helm}]}}})]
      (is (contains? tokens "ornament:motto"))
      (is (contains? tokens "ornament:slogan"))
      (is (contains? tokens "ornament:crown"))
      (is (contains? tokens "ornament:helm"))
      (testing "crown still also emits charge: (it is a charge type)"
        (is (contains? tokens "charge:crown")))))
  (testing "empty :elements means no ornament/helm tokens"
    (let [tokens (facets {:achievement
                          {:coat-of-arms {:field {:type :heraldry.field.type/plain
                                                  :tincture :argent}}
                           :ornaments {:type :heraldry/ornaments :elements []}
                           :helms {:type :heraldry/helms :elements []}}})]
      (is (not (contains? tokens "ornament:helm")))
      (is (not (contains? tokens "ornament:motto"))))))

(deftest crest-inline-emission
  (testing "charges marked with the crest-charge function emit crest:<type>"
    (let [tokens (facets {:achievement
                          {:coat-of-arms {:field {:type :heraldry.field.type/plain
                                                  :tincture :argent}}
                           :helms {:type :heraldry/helms
                                   :elements [{:type :heraldry/helm
                                               :components
                                               [{:type :heraldry.charge.type/lion
                                                 :function :heraldry.charge.function/crest-charge
                                                 :variant {:id "charge:abc" :version 1}}]}]}}})]
      (is (contains? tokens "crest:lion"))
      (testing "the same charge is also reported under charge:"
        (is (contains? tokens "charge:lion")))))
  (testing "non-crest charges do NOT emit crest:"
    (let [tokens (facets {:achievement
                          {:coat-of-arms
                           {:field {:type :heraldry.field.type/plain
                                    :tincture :argent
                                    :components
                                    [{:type :heraldry.charge.type/lion}]}}}})]
      (is (not (contains? tokens "crest:lion"))))))

(deftest crest-variants-extracted-for-backend
  (let [variants (facets/crest-variants
                  {:achievement
                   {:helms {:type :heraldry/helms
                            :elements
                            [{:type :heraldry/helm
                              :components
                              [{:type :heraldry.charge.type/lion
                                :function :heraldry.charge.function/crest-charge
                                :variant {:id "charge:abc" :version 1}}
                               {:type :heraldry.charge.type/helmet
                                :function :heraldry.charge.function/helmet
                                :variant {:id "charge:def" :version 2}}]}]}}})]
    (testing "only crest-tagged variants are returned, not the helmet"
      (is (= #{{:id "charge:abc" :version 1}} variants)))))

(deftest skips-uninteresting-tinctures
  (let [tokens (facets {:achievement
                        {:coat-of-arms
                         {:field {:type :heraldry.field.type/plain
                                  :tincture :none}}}})]
    (is (not (contains? tokens "tincture:none")))
    (is (not (contains? tokens "field:none")))
    (testing "but main-field:plain is still emitted from the type"
      (is (contains? tokens "main-field:plain")))))

(deftest version-is-an-int
  (is (integer? facets/version))
  (is (pos? facets/version)))

(deftest split-input-separates-facets-from-phrases
  (testing "pure free text"
    (is (= {:phrase-text "smith arms" :facets []}
           (facets/split-input "smith arms"))))
  (testing "facet tokens routed to :facets"
    (is (= {:phrase-text "smith" :facets ["tincture:or" "charge:lion"]}
           (facets/split-input "smith tincture:or charge:lion"))))
  (testing "facet tokens are lowercased"
    (is (= {:phrase-text "" :facets ["main-field:per-pale"]}
           (facets/split-input "MAIN-FIELD:PER-PALE"))))
  (testing "unknown key stays a phrase"
    (is (= {:phrase-text "unknown:lion" :facets []}
           (facets/split-input "unknown:lion"))))
  (testing "empty/blank input"
    (is (= {:phrase-text "" :facets []} (facets/split-input "")))
    (is (= {:phrase-text "" :facets []} (facets/split-input nil))))
  (testing "trailing colon is not a facet"
    (is (= {:phrase-text "charge:" :facets []}
           (facets/split-input "charge:")))))

(deftest slugify-name
  (is (= "lion" (facets/slugify-name "lion")))
  (is (= "north-american-bald-eagle"
         (facets/slugify-name "North American bald  eagle")))
  (is (nil? (facets/slugify-name nil))))
