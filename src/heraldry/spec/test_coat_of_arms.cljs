(ns heraldry.spec.test-coat-of-arms
  (:require [cljs.spec.alpha :as s]
            [cljs.test :refer-macros [deftest are]]
            [heraldry.spec.coat-of-arms]))

(deftest valid-fields
  (are [spec form] (do
                     (s/explain spec form)
                     (s/valid? spec form))

    :heraldry/field {:component :field
                     :content {:tincture :azure}}

    :heraldry/field {:component :field
                     :division {:type :per-pale
                                :line {:type :invected
                                       :eccentricity 1.3
                                       :width 2
                                       :offset 0.2
                                       :flipped? false}
                                :fields [{:component :field
                                          :content {:tincture :azure}}
                                         {:component :field
                                          :content {:tincture :or}}]
                                :hints {:outline? true}}}

    :heraldry/field {:component :field
                     :content {:tincture :azure}
                     :components [{:component :ordinary
                                   :type :pale
                                   :field {:component :field
                                           :content {:tincture :azure}}}
                                  {:component :charge
                                   :type :lion
                                   :attitude :rampant
                                   :facing :reguardant
                                   :field {:component :field
                                           :content {:tincture :azure}}}]}))

(deftest invalid-fields
  (are [spec form] (not (s/valid? spec form))

    :heraldry/field {:component :field}

    :heraldry/field {:component :charge
                     :content {:tincture :or}}

    :heraldry/field {:component :field
                     :division {:type :does-not-exist}}

    :heraldry/field {:component :field
                     :content {:tincture :azure}
                     :components [{:component :field
                                   :content {:tincture :or}}]}))

(deftest valid-ordinaries
  (are [spec form] (do
                     (s/explain spec form)
                     (s/valid? spec form))

    :heraldry/ordinary {:component :ordinary
                        :type :pale
                        :field {:component :field
                                :content {:tincture :azure}}}

    :heraldry/ordinary {:component :ordinary
                        :type :fess
                        :field {:component :field
                                :content {:tincture :azure}}
                        :geometry {:size 50}
                        :origin {:point :fess
                                 :offset-x -5
                                 :offset-y 5}
                        :line {:type :engrailed
                               :eccentricity 1.3
                               :width 2
                               :offset 0.2
                               :flipped? false}
                        :opposite-line {:type :engrailed
                                        :eccentricity 1.3
                                        :width 2
                                        :offset 0.2
                                        :flipped? true}}))

(deftest invalid-ordinaries
  (are [spec form] (not (s/valid? spec form))

    :heraldry/ordinary {:component :ordinary}

    :heraldry/ordinary {:component :field
                        :type :pale
                        :field {:component :field
                                :content {:tincture :azure}}}

    :heraldry/ordinary {:component :ordinary
                        :type :does-not-exist
                        :field {:component :field
                                :content {:tincture :azure}}}

    :heraldry/ordinary {:component :ordinary
                        :type :fess
                        :field {:component :field}}))

(deftest valid-charges
  (are [spec form] (do
                     (s/explain spec form)
                     (s/valid? spec form))

    :heraldry/charge {:component :charge
                      :type :lion
                      :attitude :rampant
                      :facing :reguardant
                      :field {:component :field
                              :content {:tincture :azure}}}

    :heraldry/charge {:component :charge
                      :type :roundel
                      :field {:component :field
                              :content {:tincture :azure}}
                      :geometry {:size 50}
                      :origin {:point :fess
                               :offset-x -5
                               :offset-y 5}}))

(deftest invalid-charges
  (are [spec form] (not (s/valid? spec form))

    :heraldry/charge {:component :charge}

    :heraldry/charge {:component :field
                      :type :lion
                      :field {:component :field
                              :content {:tincture :azure}}}

    :heraldry/charge {:component :charge
                      :type :wolf
                      :field {:component :field}}

    :heraldry/charge {:component :charge
                      :type :wolf
                      :attitude :foobar
                      :field {:component :field}}))

(deftest valid-coat-of-arms
  (are [spec form] (do
                     (s/explain spec form)
                     (s/valid? spec form))

    :heraldry/coat-of-arms {:spec-version 1
                            :component :coat-of-arms
                            :escutcheon :heater
                            :field {:component :field
                                    :content {:tincture :azure}}}

    :heraldry/coat-of-arms {:spec-version 1
                            :component :coat-of-arms
                            :escutcheon :polish
                            :field {:component :field
                                    :division {:type :per-pale
                                               :line {:type :invected
                                                      :eccentricity 1.3
                                                      :width 2
                                                      :offset 0.2
                                                      :flipped? false}
                                               :fields [{:component :field
                                                         :content {:tincture :azure}}
                                                        {:component :field
                                                         :content {:tincture :or}}]
                                               :hints {:outline? true}}}}))

(deftest invalid-coat-of-arms
  (are [spec form] (not (s/valid? spec form))

    :heraldry/coat-of-arms {:spec-version 1
                            :component :field
                            :escutcheon :heater
                            :field {:component :field
                                    :content {:tincture :azure}}}

    :heraldry/coat-of-arms {:spec-version 1
                            :component :coat-of-arms
                            :escutcheon :heater}

    :heraldry/coat-of-arms {:spec-version 1
                            :component :coat-of-arms
                            :field {:component :field
                                    :content {:tincture :azure}}}

    :heraldry/coat-of-arms {:spec-version 1
                            :component :coat-of-arms
                            :escutcheon :does-not-exist
                            :field {:component :field
                                    :content {:tincture :azure}}}

    :heraldry/coat-of-arms {:component :coat-of-arms
                            :escutcheon :heater
                            :field {:component :field
                                    :content {:tincture :azure}}}))

(deftest valid-render-options
  (are [spec form] (do
                     (s/explain spec form)
                     (s/valid? spec form))

    :heraldry/render-options {:component :render-options}

    :heraldry/render-options {:component :render-options
                              :mode :colours}

    :heraldry/render-options {:component :render-options
                              :outline? false}

    :heraldry/render-options {:component :render-options
                              :squiggly true}))

(deftest invalid-render-options
  (are [spec form] (not (s/valid? spec form))

    :heraldry/render-options {:component :field}

    :heraldry/render-options {:component :render-options
                              :mode :does-not-exist}

    :heraldry/render-options {:component :render-options
                              :outline? 5}

    :heraldry/render-options {:component :render-options
                              :squiggly? "foo"}))
