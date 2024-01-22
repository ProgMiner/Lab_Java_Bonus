#!/usr/bin/env python3

import matplotlib.pyplot as plt
import pandas as pd
import numpy as np
import os


for param in ['array_size', 'clients', 'request_delta']:
    raw_tables = dict()

    for arch in ['block', 'nonblock', 'async']:
        raw_tables[arch] = pd.read_csv(f'./report/{param}/{arch}/data.csv', index_col=False)

    tables = dict()
    for i in range(1, 4):
        if len(set([x.shape[0] for x in raw_tables.values()])) != 1:
            raise ValueError('different numbers of rows')

        names = set([x.columns[i] for x in raw_tables.values()])

        if len(names) != 1:
            raise ValueError(f'different names of columns: {names}')

        name = list(names)[0]

        param_columns = [x.iloc[:, 0] for x in raw_tables.values()]

        if len(set([(x.name, tuple(x)) for x in param_columns])) != 1:
            raise ValueError(f'different parameter columns: {param_columns}')

        param_column = list(param_columns)[0]

        tables[name] = pd.concat(
            [param_column] \
            + [x.iloc[:, i] for x in raw_tables.values()
        ], axis=1)

        tables[name].columns = [param_column.name] + [x for x in raw_tables.keys()]
        tables[name].set_index(param_column.name, inplace=True)

    y_limits = (
        min([x.to_numpy().min() for x in tables.values()]),
        max([x.to_numpy().max() for x in tables.values()]),
    )

    os.makedirs(f'./plots/{param}', mode=0o755, exist_ok=True)
    for name, table in tables.items():
        table.plot(title=name, ylim=y_limits)

        plt.savefig(f'./plots/{param}/{name}.png')
