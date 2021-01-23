(ns heraldry.spec.test
  (:require [cljs.spec.alpha :as s]
            [cljs.test :refer-macros [deftest is are]]
            [heraldry.spec.core]))

(deftest valid-fields
  (are [spec form] (do
                     (s/explain spec form)
                     (s/valid? spec form))

    :heraldry/field {:component :field
                     :content   {:tincture :azure}}

    :heraldry/field {:component :field
                     :division  {:type   :per-pale
                                 :line   {:type         :invected
                                          :eccentricity 1.3
                                          :width        2
                                          :offset       0.2
                                          :flipped?     false}
                                 :fields [{:component :field
                                           :content   {:tincture :azure}}
                                          {:component :field
                                           :content   {:tincture :or}}]
                                 :hints  {:outline? true}}}))

(deftest invalid-fields
  (are [spec form] (not (s/valid? spec form))

    :heraldry/field {:component :field}

    :heraldry/field {:component :something-else}

    :heraldry/field {:component :field
                     :division  {:type :does-not-exist}}))

(deftest valid-ordinaries
  (are [spec form] (do
                     (s/explain spec form)
                     (s/valid? spec form))

    :heraldry/ordinary {:component :ordinary
                        :type      :pale
                        :field     {:component :field
                                    :content   {:tincture :azure}}}

    :heraldry/ordinary {:component     :ordinary
                        :type          :fess
                        :field         {:component :field
                                        :content   {:tincture :azure}}
                        :geometry      {:size 50}
                        :origin        {:point    :fess
                                        :offset-x -5
                                        :offset-y 5}
                        :line          {:type         :engrailed
                                        :eccentricity 1.3
                                        :width        2
                                        :offset       0.2
                                        :flipped?     false}
                        :opposite-line {:type         :engrailed
                                        :eccentricity 1.3
                                        :width        2
                                        :offset       0.2
                                        :flipped?     true}}))

(deftest invalid-ordinaries
  (are [spec form] (not (s/valid? spec form))

    :heraldry/ordinary {:component :ordinary}

    :heraldry/ordinary {:component :something-else
                        :type      :pale
                        :field     {:component :field
                                    :content   {:tincture :azure}}}

    :heraldry/ordinary {:component :ordinary
                        :type      :does-not-exist
                        :field     {:component :field
                                    :content   {:tincture :azure}}}

    :heraldry/ordinary {:component :ordinary
                        :type      :fess
                        :field     {:component :field}}))
