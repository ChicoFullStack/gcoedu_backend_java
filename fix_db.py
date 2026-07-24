import psycopg2

def fix_db():
    print("Conectando ao banco de dados...")
    conn = psycopg2.connect("postgresql://postgres:postgres@179.197.232.2:5432/postgres")
    conn.autocommit = True
    cur = conn.cursor()
    
    cur.execute("SELECT table_schema FROM information_schema.tables WHERE table_name = 'answer_sheet_gabaritos'")
    schemas = [row[0] for row in cur.fetchall()]
    
    if not schemas:
        print("A tabela answer_sheet_gabaritos não foi encontrada em nenhum schema.")
        return

    for schema in schemas:
        print(f"Atualizando schema: {schema}")
        try:
            cur.execute(f"ALTER TABLE {schema}.answer_sheet_gabaritos ADD COLUMN IF NOT EXISTS use_blocks BOOLEAN DEFAULT FALSE;")
            cur.execute(f"ALTER TABLE {schema}.answer_sheet_gabaritos ADD COLUMN IF NOT EXISTS separate_by_subject BOOLEAN DEFAULT FALSE;")
            cur.execute(f"ALTER TABLE {schema}.answer_sheet_gabaritos ADD COLUMN IF NOT EXISTS blocks_config JSONB;")
            cur.execute(f"ALTER TABLE {schema}.answer_sheet_gabaritos ADD COLUMN IF NOT EXISTS correct_answers JSONB;")
            cur.execute(f"ALTER TABLE {schema}.answer_sheet_gabaritos ADD COLUMN IF NOT EXISTS template_block_1 BYTEA;")
            cur.execute(f"ALTER TABLE {schema}.answer_sheet_gabaritos ADD COLUMN IF NOT EXISTS template_block_2 BYTEA;")
            print(f"Schema '{schema}' atualizado com sucesso!")
        except Exception as e:
            print(f"Erro no schema '{schema}': {e}")
            
    cur.close()
    conn.close()
    print("Pronto!")

if __name__ == '__main__':
    fix_db()
