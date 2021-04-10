(ns heraldry.spec.test-coat-of-arms
  (:require [cljs.spec.alpha :as s]
            [cljs.test :refer-macros [deftest are]]
            [heraldry.spec.coat-of-arms]))

(defn check-spec [spec form]
  (let [explain-output (with-out-str (s/explain spec form))
        conforms?      (s/valid? spec form)]
    (when-not conforms?
      (println explain-output))
    conforms?))

(deftest valid-fields
  (are [spec form] (check-spec spec form)

    :heraldry/field {:type     :heraldry.field.type/plain
                     :tincture :azure}

    :heraldry/field {:type   :heraldry.field.type/per-pale
                     :origin {:point    :fess
                              :offset-x 0
                              :offset-y nil}
                     :line   {:type         :invected
                              :eccentricity 1.3
                              :width        nil
                              :offset       0.2
                              :flipped?     false}
                     :fields [{:type     :heraldry.field.type/plain
                               :tincture :azure}
                              {:type     :heraldry.field.type/plain
                               :tincture :or}
                              {:type  :heraldry.field.type/ref
                               :index 0}]
                     :hints  {:outline? true}}

    :heraldry/field {:type       :heraldry.field.type/plain
                     :tincture   :azure
                     :components [{:type  :heraldry.ordinary.type/pale
                                   :field {:type     :heraldry.field.type/plain
                                           :tincture :azure}}
                                  {:type     :heraldry.charge.type/lion
                                   :attitude :rampant
                                   :facing   :reguardant
                                   :field    {:type     :heraldry.field.type/plain
                                              :tincture :azure}}]}))

(deftest invalid-fields
  (are [spec form] (not (s/valid? spec form))

    :heraldry/field {}

    :heraldry/field {:type     :heraldry.charge.type/roundel
                     :tincture :or}

    :heraldry/field {:type     :heraldry.field.type/per-pale
                     :tincture :azure}

    :heraldry/field {:type   :heraldry.field.type/plain
                     :fields [{:type     :heraldry.field.type/plain
                               :tincture :azure}
                              {:type     :heraldry.field.type/plain
                               :tincture :or}
                              {:type  :heraldry.field.type/ref
                               :index 1}]}

    :heraldry/field {:type :does-not-exist}

    :heraldry/field {:type       :heraldry.field.type/plain
                     :tincture   :azure
                     ;; the component here is just a field, not a charge or ordinary
                     :components [{:type     :heraldry.field.type/plain
                                   :tincture :or}]}))

(deftest valid-ordinaries
  (are [spec form] (check-spec spec form)

    :heraldry/ordinary {:type  :heraldry.ordinary.type/pale
                        :field {:type     :heraldry.field.type/plain
                                :tincture :azure}}

    :heraldry/ordinary {:type          :heraldry.ordinary.type/fess
                        :field         {:type     :heraldry.field.type/plain
                                        :tincture :azure}
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

    :heraldry/ordinary {}

    :heraldry/ordinary {:type  :heraldry.field.type/per-pale
                        :field {:type     :heraldry.field.type/plain
                                :tincture :azure}}

    :heraldry/ordinary {:type  :does-not-exist
                        :field {:type     :heraldry.field.type/plain
                                :tincture :azure}}

    :heraldry/ordinary {:type  :fess
                        :field {}}))

(deftest valid-charges
  (are [spec form] (check-spec spec form)

    :heraldry/charge {:type     :heraldry.charge.type/roundel
                      :attitude nil
                      :facing   nil
                      :variant  nil
                      :field    {:type     :heraldry.field.type/plain
                                 :tincture :azure}}

    :heraldry/charge {:type     :heraldry.charge.type/lion
                      :attitude :rampant
                      :facing   :reguardant
                      :field    {:type     :heraldry.field.type/plain
                                 :tincture :azure}}

    :heraldry/charge {:type     :heraldry.charge.type/roundel
                      :field    {:type     :heraldry.field.type/plain
                                 :tincture :azure}
                      :tincture {:eyes-and-teeth :or
                                 :shadow         0.5
                                 :highlight      0.5
                                 :primary        :or}
                      :geometry {:size 50}
                      :origin   {:point    :fess
                                 :offset-x -5
                                 :offset-y 5}}))

(deftest invalid-charges
  (are [spec form] (not (s/valid? spec form))

    :heraldry/charge {}

    :heraldry/charge {:type  :heraldry.charge.type/wolf
                      :field {}}

    :heraldry/charge {:type     :heraldry.charge.type/wolf
                      :attitude :foobar
                      :field    {:type     :heraldry.field.type/plain
                                 :tincture :azure}}

    :heraldry/charge {:type     :heraldry.charge.type/wolf
                      :attitude :rampant
                      :field    {:type     :heraldry.field.type/plain
                                 :tincture :azure}
                      :tincture {:shadow true}}

    :heraldry/charge {:type     :heraldry.charge.type/wolf
                      :attitude :rampant
                      :field    {:type     :heraldry.field.type/plain
                                 :tincture :azure}
                      :tincture {:eyes-and-teeth 0.5}}

    :heraldry/charge {:type     :heraldry.charge.type/wolf
                      :attitude :rampant
                      :field    {:type     :heraldry.field.type/plain
                                 :tincture :azure}
                      :tincture {:highlight :or}}))

(deftest valid-coat-of-arms
  (are [spec form] (check-spec spec form)

    :heraldry/coat-of-arms {:spec-version 1
                            :type         :coat-of-arms
                            :escutcheon   :heater
                            :field        {:type     :heraldry.field.type/plain
                                           :tincture :azure}}

    :heraldry/coat-of-arms {:spec-version 1
                            :type         :coat-of-arms
                            :escutcheon   :polish
                            :field        {:type   :per-pale
                                           :line   {:type         :invected
                                                    :eccentricity 1.3
                                                    :width        2
                                                    :offset       0.2
                                                    :flipped?     false}
                                           :fields [{:type     :heraldry.field.type/plain
                                                     :tincture :azure}
                                                    {:type     :heraldry.field.type/plain
                                                     :tincture :or}]
                                           :hints  {:outline? true}}}))

(deftest invalid-coat-of-arms
  (are [spec form] (not (s/valid? spec form))

    :heraldry/coat-of-arms {}

    :heraldry/coat-of-arms {:spec-version 1
                            :escutcheon   :heater}

    :heraldry/coat-of-arms {:spec-version 1
                            :field        {:type     :heraldry.field.type/plain
                                           :tincture :azure}}

    :heraldry/coat-of-arms {:spec-version 1
                            :escutcheon   :does-not-exist
                            :field        {:type     :heraldry.field.type/plain
                                           :tincture :azure}}

    :heraldry/coat-of-arms {:escutcheon :heater
                            :field      {:type     :heraldry.field.type/plain
                                         :tincture :azure}}))

(deftest valid-render-options
  (are [spec form] (check-spec spec form)

    :heraldry/render-options {}

    :heraldry/render-options {:mode :colours}

    :heraldry/render-options {:outline? false}

    :heraldry/render-options {:squiggly true}

    :heraldry/render-options {:escutcheon-override :heater}))

(deftest invalid-render-options
  (are [spec form] (not (s/valid? spec form))

    :heraldry/render-options {:mode :does-not-exist}

    :heraldry/render-options {:outline? 5}

    :heraldry/render-options {:squiggly? "foo"}

    :heraldry/render-options {:escutcheon-override :does-not-exist}))

